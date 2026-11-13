import { useEffect, useState } from "react";
import { api } from "../../services/api";
import { Availability, Modal } from "../../components/shared";
import { money } from "../../lib/constants";

export function MatchDetail({ card, close, run, busy }) {
  const [d, setD] = useState(null);
  const [error, setError] = useState("");
  useEffect(() => {
    api(`/matching/pairs/${card.studentId}/${card.tutorId}`)
      .then(setD)
      .catch((e) => setError(e.message));
  }, [card.studentId, card.tutorId]);
  const decide = (confirm) =>
    run(async () =>
      setD(
        await api("/matching/confirm", {
          method: "POST",
          body: { studentId: card.studentId, tutorId: card.tutorId, confirm },
        }),
      ),
    );
  const t = d?.tutor;
  const s = d?.student;
  return (
    <Modal close={close} label="Hồ sơ chi tiết">
      {error && <p className="error" role="alert">{error}</p>}
      {!d && !error && <p role="status">Đang tải…</p>}
      {t && (
        <>
          <h2>{t.display_name}</h2>
          <p>{t.subjects.join(" · ")} · Lớp {t.grades.join(", ")} · {money(t.hourly_rate)}/giờ</p>
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
      {d?.myDecision === "CONFIRM" && !d.connected && d.status === "MATCHED" && (
        <p role="status">
          Bạn đã xác nhận. Đang chờ bên kia
          {d.expiresAt && ` đến ${new Date(d.expiresAt).toLocaleString("vi-VN")}`}.
        </p>
      )}
      {d && d.status !== "MATCHED" && !d.connected && <p role="status">Cặp đã đóng.</p>}
    </Modal>
  );
}