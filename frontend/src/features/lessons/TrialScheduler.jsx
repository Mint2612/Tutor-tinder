import { useEffect, useRef, useState } from "react";
import { api } from "../../services/api";
import { useWorkspace } from "../../hooks/useWorkspace";

const labels = { PENDING: "Chờ phản hồi", CONFIRMED: "Đã chốt", CANCELLED: "Đã hủy", EXPIRED: "Hết hạn" };
const trialTime = (value) => new Date(value).toLocaleString("vi-VN");
export function TrialScheduler({ studentId, tutorId, mode }) {
  const { user } = useWorkspace();
  const [items, setItems] = useState(null);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const [slots, setSlots] = useState([""]);
  const [counter, setCounter] = useState(false);
  const [cancelling, setCancelling] = useState(false);
  const [reason, setReason] = useState("");
  const [slot, setSlot] = useState("");
  const [modeChoice, setModeChoice] = useState("");
  const [refresh, setRefresh] = useState(0);
  const lock = useRef(false);
  const path = `/trials/pairs/${studentId}/${tutorId}`;
  useEffect(() => {
    const controller = new AbortController();
    api(path, { signal: controller.signal }).then(setItems).catch((e) => {
      if (e.name !== "AbortError") setError(e.message);
    });
    return () => controller.abort();
  }, [path, refresh]);
  const active = items?.find((t) => ["PENDING", "CONFIRMED"].includes(t.status));
  const act = async (body, proposal = false) => {
    if (lock.current) return;
    lock.current = true; setBusy(true); setError("");
    try {
      await api(proposal ? "/trials" : `/trials/${active.id}`, {
        method: proposal ? "POST" : "PUT",
        body: proposal ? { studentId, tutorId, ...body } : { version: active.version, ...body },
      });
      setItems(await api(path));
      setCounter(false); setCancelling(false); setReason(""); setSlot(""); setSlots([""]);
    } catch (e) { setError(e.message); }
    finally { lock.current = false; setBusy(false); }
  };
  const canReply = active?.status === "PENDING" && active.proposerRole !== user.role;
  const submitSlots = (e) => {
    e.preventDefault();
    const times = slots.map((value) => new Date(value));
    if (times.some((d) => Number.isNaN(d.getTime()))) { setError("Vui lòng chọn đủ giờ hợp lệ."); return; }
    act({ ...(active ? { action: "COUNTER" } : { mode: modeChoice || mode || items?.[0]?.mode || "ONLINE" }), slots: times.map((d) => d.toISOString()) }, !active);
  };
  return <section className="trial-scheduler" aria-label="Đặt lịch học thử">
    <h3>Lịch học thử</h3>
    <p className="fineprint">Giờ hiển thị theo múi giờ thiết bị. Đề xuất 1–3 giờ sau ít nhất một giờ; phản hồi trong 3 ngày và trước giờ sớm nhất.</p>
    {error && <p className="error" role="alert">{error}</p>}
    <button className="secondary small" disabled={busy} onClick={() => { setError(""); setItems(null); setRefresh((n) => n + 1); }}>Cập nhật lịch</button>
    {!items && !error && <p role="status">Đang tải lịch học thử…</p>}
    {items && !items.length && <p>Chưa có đề xuất học thử.</p>}
    {active && <div className="panel">
      <strong role="status">{labels[active.status]}</strong>
      <p>Hình thức học: {active.mode === "ONLINE" ? "Trực tuyến" : "Trực tiếp"}</p>
      {active.status === "CONFIRMED" ? <p>Giờ đã chốt: {trialTime(active.chosenAt)}</p> : <>
        <p>{canReply ? "Bên kia đề xuất:" : "Bạn đã đề xuất; đang chờ bên kia phản hồi."}</p>
        <ul>{active.slots.map((s) => <li key={s}>{trialTime(s)}</li>)}</ul>
        <p className="fineprint">Hạn phản hồi: {trialTime(active.expiresAt)}</p>
      </>}
      {canReply && !counter && <form onSubmit={(e) => { e.preventDefault(); act({ action: "ACCEPT", slot }); }}>
        <label>Chọn giờ học thử
          <select required value={slot} disabled={busy} onChange={(e) => setSlot(e.target.value)}>
            <option value="">Chọn một giờ</option>
            {active.slots.map((s) => <option key={s} value={s}>{trialTime(s)}</option>)}
          </select>
        </label>
        <div className="row"><button disabled={busy || !slot}>Chốt giờ học thử</button>
          <button type="button" className="secondary" disabled={busy} onClick={() => setCounter(true)}>Đề xuất giờ khác</button></div>
      </form>}
      {!cancelling ? <button className="danger-button" disabled={busy} onClick={() => setCancelling(true)}>Hủy lịch học thử</button>
        : <form onSubmit={(e) => { e.preventDefault(); act({ action: "CANCEL", reason }); }}>
          <label>Lý do hủy lịch<textarea required maxLength={500} value={reason} disabled={busy} onChange={(e) => setReason(e.target.value)} /></label>
          <div className="row"><button className="danger-button" disabled={busy || !reason.trim()}>Xác nhận hủy lịch</button>
            <button type="button" className="secondary" disabled={busy} onClick={() => setCancelling(false)}>Giữ lịch</button></div>
        </form>}
    </div>}
    {items && (!active || counter) && <form onSubmit={submitSlots}>
      <h4>{counter ? "Đề xuất giờ khác" : "Đề xuất lịch học thử"}</h4>
      {!active && <label>Hình thức học thử<select aria-label="Hình thức học thử" disabled={busy}
        value={modeChoice || mode || items?.[0]?.mode || "ONLINE"} onChange={(e) => setModeChoice(e.target.value)}>
        <option value="ONLINE">Trực tuyến</option><option value="IN_PERSON">Trực tiếp</option>
      </select></label>}
      {slots.map((value, index) => <label key={index}>Giờ đề xuất {index + 1}
        <input type="datetime-local" required disabled={busy} value={value} onChange={(e) => setSlots((values) => values.map((v, i) => i === index ? e.target.value : v))} />
      </label>)}
      <div className="row">
        {slots.length < 3 && <button type="button" className="secondary" disabled={busy} onClick={() => setSlots((s) => [...s, ""])}>Thêm giờ đề xuất</button>}
        {slots.length > 1 && <button type="button" className="secondary" disabled={busy} onClick={() => setSlots((s) => s.slice(0, -1))}>Bớt giờ đề xuất</button>}
        <button disabled={busy}>{busy ? "Đang gửi…" : "Gửi đề xuất học thử"}</button>
        {counter && <button type="button" className="secondary" disabled={busy} onClick={() => setCounter(false)}>Quay lại</button>}
      </div>
    </form>}
    {items?.filter((t) => !["PENDING", "CONFIRMED"].includes(t.status)).map((t) => <p key={t.id}>
      {labels[t.status]} · {t.slots.map(trialTime).join("; ")}{t.cancellationReason && ` · Lý do: ${t.cancellationReason}`}
    </p>)}
  </section>;
}
