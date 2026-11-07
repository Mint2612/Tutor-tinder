import { useRef, useState } from "react";
import { Avatar, Availability } from "../../components/shared";
import { money, levelText } from "../../lib/constants";

export function SwipeCard({ recommendation: r, onSwipe, onDetail, busy }) {
  const [offset, setOffset] = useState(0),
    start = useRef(null);
  const t = r.tutor;
  return (
    <div className="swipe-area">
      <article
        className="tutor-card"
        style={{
          transform: `translateX(${offset}px) rotate(${offset / 22}deg)`,
        }}
        onPointerDown={(e) => {
          if (
            busy ||
            e.button !== 0 ||
            e.target.closest("button, summary, a, input")
          )
            return;
          start.current = e.clientX;
          e.currentTarget.setPointerCapture(e.pointerId);
        }}
        onPointerMove={(e) => {
          if (start.current !== null)
            setOffset(Math.max(-140, Math.min(140, e.clientX - start.current)));
        }}
        onPointerUp={(e) => {
          if (start.current === null) return;
          const direction = e.clientX - start.current;
          setOffset(0);
          start.current = null;
          if (Math.abs(direction) > 75 && !busy) onSwipe(direction > 0);
        }}
        onPointerCancel={() => {
          start.current = null;
          setOffset(0);
        }}
      >
        <div className="card-visual">
          <div className="card-pattern"></div>
          <Avatar name={t.display_name} photo={t.photo_url} large />
          <span className="verified">✓ Đã xác minh</span>
          <span className="card-subject">{t.subjects.join(" · ")}</span>
          {Math.abs(offset) > 40 && (
            <span className="swipe-stamp">
              {offset > 0 ? "QUAN TÂM" : "BỎ QUA"}
            </span>
          )}
        </div>
        <div className="card-body">
          <div className="row">
            <h2>{t.display_name}</h2>
            <span className="rate">
              {money(t.hourly_rate)}
              <small>/giờ</small>
            </span>
          </div>
          <p className="muted">
            {t.experience_years} năm kinh nghiệm · {t.province}
          </p>
          <div className="tags">
            <span>{t.teaching_style}</span>
            <span>Lớp {t.grades.join(", ")}</span>
          </div>
          <p className="availability">
            <Availability slots={t.availability} />
          </p>
          <div className="ai-note">
            <span>✦ {levelText[r.level]}</span>
            <p>{r.explanation}</p>
            <small>
              {r.source === "AI"
                ? "Có hỗ trợ từ AI"
                : "Gợi ý theo điều kiện & kinh nghiệm"}
            </small>
          </div>
          <details>
            <summary>Điểm mạnh & điều cần cân nhắc</summary>
            <ul>
              {r.strengths.map((s, i) => (
                <li key={i}>{s}</li>
              ))}
            </ul>
            <ul>
              {r.concerns.map((s, i) => (
                <li key={i}>{s}</li>
              ))}
            </ul>
          </details>
          <button
            className="text-button full"
            disabled={busy}
            onClick={onDetail}
          >
            Xem hồ sơ đầy đủ ↗
          </button>
        </div>
      </article>
      <div className="swipe-actions">
        <button
          className="skip"
          disabled={busy}
          onClick={() => onSwipe(false)}
          aria-label="Bỏ qua gia sư"
        >
          ×
        </button>
        <span>
          Bỏ qua <span className="muted">·</span> Quan tâm
        </span>
        <button
          className="like"
          disabled={busy}
          onClick={() => onSwipe(true)}
          aria-label="Quan tâm gia sư"
        >
          ♡
        </button>
      </div>
    </div>
  );
}
