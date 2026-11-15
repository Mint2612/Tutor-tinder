import { useEffect, useState } from "react";
import { api } from "../../services/api";
import { Availability, Modal } from "../../components/shared";
import { money } from "../../lib/constants";
import { TrialScheduler } from "../lessons/TrialScheduler";

export function MatchDetail({ card, close, run, busy }) {
  const [d, setD] = useState(null);
  const [error, setError] = useState("");
  const [refresh, setRefresh] = useState(0);
  useEffect(() => {
    const controller = new AbortController();
    api(`/matching/pairs/${card.studentId}/${card.tutorId}`, { signal: controller.signal })
      .then(setD)
      .catch((e) => { if (e.name !== "AbortError") setError(e.message); });
    return () => controller.abort();
  }, [card.studentId, card.tutorId, refresh]);
  const decide = (confirm) =>
    run(async () => {
      setError("");
      try { setD(
        await api("/matching/confirm", {
          method: "POST",
          body: { studentId: card.studentId, tutorId: card.tutorId, confirm },
        }),
      ); } catch (e) { setError(e.message); }
    });
  const t = d?.tutor;
  const s = d?.student;
  return (
    <Modal close={close} label="Hồ sơ chi tiết">
      {error && <p className="error" role="alert">{error} <button disabled={busy} onClick={() => { setError(""); setD(null); setRefresh((n) => n + 1); }}>Thử lại</button></p>}
      {!d && !error && <p role="status">Đang tải…</p>}
      {t && (
        <>
          <h2>{t.display_name}</h2>
          {t.photo_url && <img className="detail-photo" src={t.photo_url} alt={`Ảnh đại diện ${t.display_name}`} />}
          <p>{t.subjects.join(" · ")} · Lớp {t.grades.join(", ")} · {money(t.hourly_rate)}/giờ</p>
          <p>{t.province} · {t.modes.map((m) => m === "ONLINE" ? "Trực tuyến" : "Trực tiếp").join(" · ")}</p>
          <p>{t.background} · {t.experience_years} năm kinh nghiệm</p>
          <p>{t.teaching_style}. {t.teaching_description}</p>
          {t.achievements && <p>Tự khai: {t.achievements}</p>}
          <p><Availability slots={t.availability} /></p>
        </>
      )}
      {s && (
        <>
          <h2>Học sinh lớp {s.grade}</h2>
          <p>{s.subjects.join(" · ")} · {s.mode === "ONLINE" ? "Trực tuyến" : "Trực tiếp"} · {s.province}</p>
          <p>Ngân sách tối đa {money(s.budget)}/giờ</p>
          <div className="tags">{s.learningPreferences.map((x) => <span key={x}>{x}</span>)}</div>
          {s.needs && <p>{s.needs}</p>}
          <p><Availability slots={s.availability} /></p>
        </>
      )}
      {d?.connected && d.contact && (
        <div className="contact">
          <strong>Đã kết nối</strong>
          <p>{d.contact.email} · {d.contact.phone}</p>
        </div>
      )}
      {d && !d.connected && d.status === "MATCHED" && !d.myDecision && (
        <div className="row">
          <button disabled={busy} onClick={() => decide(true)}>Xác nhận kết nối</button>
          <button className="secondary" disabled={busy} onClick={() => decide(false)}>Từ chối</button>
        </div>
      )}
      {d?.expiresAt && !d.connected && d.status === "MATCHED" && <p className="fineprint">Hạn xác nhận kết nối: {new Date(d.expiresAt).toLocaleString("vi-VN")}</p>}
      {d?.myDecision === "CONFIRM" && !d.connected && d.status === "MATCHED" && (
        <p role="status">
          Bạn đã xác nhận. Đang chờ bên kia
          {d.expiresAt && ` đến ${new Date(d.expiresAt).toLocaleString("vi-VN")}`}.
        </p>
      )}
      {d && d.status !== "MATCHED" && !d.connected && <p role="status">Cặp đã đóng.</p>}
      {d?.connected && <TrialScheduler studentId={card.studentId} tutorId={card.tutorId} mode={d.learningMode} />}
    </Modal>
  );
}
