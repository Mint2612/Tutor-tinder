import { useRef, useState } from "react";
import { Availability } from "../../components/shared";
import { money, levelText } from "../../lib/constants";

export function MatchingCard({ card, busy, saved = false, onChoose, onDetail }){
  const start = useRef(null);
  const [offset, setOffset] = useState(0);
  const tutor = card.tutor;
  const student = card.student;
  const incoming = card.tag === "INTERESTED_IN_YOU";
  const canLike = !busy && card.canLike && !saved;
  return (
    <div className="swipe-area">
      <article className="tutor-card matching-card" style={{ transform: `translateX(${offset}px) rotate(${offset / 22}deg)` }}
        onPointerDown={(e) => {
          if (busy || saved || e.button !== 0 || e.target.closest("button, summary, a")) return;
          start.current = e.clientX;
          e.currentTarget.setPointerCapture(e.pointerId);
        }}
        onPointerMove={(e) => {
          if (start.current !== null) setOffset(Math.max(-100, Math.min(100, e.clientX - start.current)));
        }}
        onPointerUp={(e) => {
          if (start.current === null) return;
          const distance = e.clientX - start.current;
          start.current = null;
          setOffset(0);
          if (!busy && !saved && Math.abs(distance) > 75 && (distance < 0 || canLike)) onChoose(distance > 0);
        }}
        onPointerCancel={() => { start.current = null; setOffset(0); }}>
        <div className="card-body">
          <div className="tags">
            <span>{incoming ? "♡ Quan tâm bạn" : "✦ Đề xuất"}</span>
            {card.status === "MATCHED" && <span className="verified">✓ Đã Match</span>}
          </div>
          <h2>{tutor ? tutor.displayName : `Học sinh lớp ${student.grade}`}</h2>
          {tutor ? <>
            <span className="verified">✓ {tutor.verificationBadge}</span>
            <p>{tutor.subjects.join(" · ")} · Lớp {tutor.grades.join(", ")}</p>
            <p>{tutor.experienceYears} năm kinh nghiệm · {tutor.province}</p>
            <p>{money(tutor.hourlyRate)}/giờ · {tutor.modes.map((m) => m === "ONLINE" ? "Trực tuyến" : "Trực tiếp").join(", ")}</p>
          </> : <>
            <p>{student.subjects.join(" · ")} · {student.mode === "ONLINE" ? "Trực tuyến" : "Trực tiếp"} · {student.province}</p>
            <p>Ngân sách tối đa {money(student.budget)}/giờ</p>
            <p>{student.needsSummary}</p>
            <div className="tags">{student.learningPreferences.map((text) => <span key={text}>{text}</span>)}</div>
            <p className="fineprint">Hồ sơ học sinh ẩn danh. Thông tin liên hệ chưa được chia sẻ.</p>
          </>}
          <p><Availability slots={(tutor || student).availability} /></p>
          <div className="ai-note">
            <span>✦ {levelText[card.level]}</span>
            <p>{card.explanation}</p>
          </div>
          <details><summary>Điểm mạnh & điều cần cân nhắc</summary>
            <ul>{card.strengths.map((text) => <li key={text}>{text}</li>)}</ul>
            <ul>{card.concerns.map((text) => <li key={text}>{text}</li>)}</ul>
          </details>
          {card.expiresAt && card.status !== "MATCHED" && <p className="fineprint">Chờ phản hồi đến {new Date(card.expiresAt).toLocaleString("vi-VN")}</p>}
          {saved && <p role="status">{card.status === "MATCHED" ? "Hai bên đã cùng OK. Đã tạo Match; thông tin liên hệ vẫn được bảo vệ." : "Bạn đã OK. Đang chờ bên kia phản hồi."}</p>}
          {saved && card.status === "MATCHED" && (
            <button className="secondary" onClick={onDetail}>Xem hồ sơ & xác nhận kết nối</button>
          )}
        </div>
      </article>
      {!saved && <>
        <div className="swipe-actions">
          <button className="skip" disabled={busy} onClick={() => onChoose(false)} aria-label="Bỏ qua">×</button>
          <span>Bỏ qua · OK</span>
          <button className="like" disabled={!canLike} onClick={() => onChoose(true)} aria-label="OK — quan tâm">♡</button>
        </div>
        {!card.canLike && <p className="error" role="status">Đã hết lượt OK cho loại thẻ này hôm nay. Thử lại vào ngày mai; bạn vẫn có thể Bỏ qua. Thẻ được giữ đến khi hết hạn.</p>}
      </>}
    </div>
  );
}
