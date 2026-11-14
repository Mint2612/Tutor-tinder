import { useState } from "react";
import { api } from "../../services/api";
import { money, levelText } from "../../lib/constants";

export function AiSuggestions({ studentId, run, busy, onChanged }) {
  const [items, setItems] = useState(null);
  const load = () =>
    run(async () => setItems(await api(`/students/${studentId}/recommendations/ai`)));
  const choose = (tutorId, interested) =>
    run(async () => {
      try {
        await api("/matching/actions", {
          method: "POST",
          body: { studentId, tutorId, interested },
        });
        setItems((list) => list.filter((r) => r.tutor.id !== tutorId));
      } finally {
        onChanged();
      }
    });
  return (
    <section className="panel">
      <h2>✦ Gợi ý bằng AI</h2>
      <p className="muted">
        Lọc theo điều kiện bắt buộc, rồi AI so mô tả nhu cầu của con với cách dạy của
        gia sư để chọn ra 5 người gần nhất.
      </p>
      <button disabled={busy || !studentId} onClick={load}>
        {items ? "Làm mới gợi ý AI" : "Gợi ý 5 gia sư bằng AI"}
      </button>
      {items && !items.length && (
        <p className="muted">Chưa có gia sư đáp ứng đủ điều kiện bắt buộc.</p>
      )}
      <div className="grid">
        {items?.map((r) => (
          <article className="panel" key={r.tutor.id}>
            <h3>{r.tutor.display_name}</h3>
            <span className="status">{levelText[r.level]}</span>
            <p>
              {r.tutor.subjects.join(" · ")} · {money(r.tutor.hourly_rate)}/giờ ·{" "}
              {r.tutor.experience_years} năm kinh nghiệm
            </p>
            <p className="muted">{r.tutor.teaching_style}</p>
            <p className="fineprint">
              {r.explanation}{" "}
              {r.source === "AI" ? "(AI)" : "(Theo luật: AI tạm thời không khả dụng)"}
            </p>
            <div className="row">
              <button disabled={busy} onClick={() => choose(r.tutor.id, true)}>OK</button>
              <button className="secondary" disabled={busy} onClick={() => choose(r.tutor.id, false)}>
                Bỏ qua
              </button>
            </div>
          </article>
        ))}
      </div>
    </section>
  );
}