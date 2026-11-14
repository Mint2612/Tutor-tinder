import { useRef, useState } from "react";
import { Availability, Avatar } from "../../components/shared";
import { money, levelText } from "../../lib/constants";

// "6, 7, 8, 9" -> "6–9", "1, 2, 5" -> "1–2, 5"
function gradeRanges(grades) {
  const sorted = [...grades].sort((a, b) => a - b);
  const parts = [];
  for (let i = 0; i < sorted.length;) {
    let j = i;
    while (sorted[j + 1] === sorted[j] + 1) j++;
    parts.push(j > i ? `${sorted[i]}–${sorted[j]}` : `${sorted[i]}`);
    i = j + 1;
  }
  return parts.join(", ");
}
const modeText = (modes) => modes.map((m) => (m === "ONLINE" ? "Trực tuyến" : "Trực tiếp")).join(", ");

// compact: tóm tắt ngắn cho danh sách gợi ý (không có mô tả mức phù hợp, điểm mạnh, lịch chi tiết).
export function MatchingCard({ card, busy, saved = false, swipe = true, compact = false, onChoose, onDetail }){
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
          if (!swipe || busy || saved || e.button !== 0 || e.target.closest("button, summary, a")) return;
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
        <div className={`card-body${compact ? " compact" : ""}`}>
          <div className="tags">
            {card.status !== "MATCHED" && (incoming || !compact) && <span>{incoming ? "♡ Quan tâm bạn" : "✦ Đề xuất"}</span>}
            {card.status === "MATCHED" && <span className="verified">✓ Đã Match</span>}
          </div>
          {tutor ? <div className="matching-tutor-heading">
            <Avatar name={tutor.displayName} photo={tutor.photoUrl} />
            <h2>{tutor.displayName}</h2>
          </div> : <h2>{`Học sinh lớp ${student.grade}`}</h2>}
          {tutor ? <>
            <span className="verified">✓ {tutor.verificationBadge}</span>
            <p>{tutor.subjects.join(" · ")} · Lớp {gradeRanges(tutor.grades)}</p>
            <p>{money(tutor.hourlyRate)}/giờ · {tutor.experienceYears} năm kinh nghiệm</p>
            <p>{tutor.province} · {modeText(tutor.modes)}</p>
          </> : <>
            <p>{student.subjects.join(" · ")} · {modeText([student.mode])} · {student.province}</p>
            <p>Ngân sách tối đa {money(student.budget)}/giờ</p>
            {!compact && <p>{student.needsSummary}</p>}
            <div className="tags">{(compact ? student.learningPreferences.slice(0, 3) : student.learningPreferences).map((text) => <span key={text}>{text}</span>)}</div>
            {!compact && <p className="fineprint">Hồ sơ học sinh ẩn danh. Thông tin liên hệ chưa được chia sẻ.</p>}
          </>}
          {!compact && <>
            <p><Availability slots={(tutor || student).availability} /></p>
            <div className="ai-note">
              <span>✦ {levelText[card.level]}</span>
              <p>{card.explanation}</p>
            </div>
            <details><summary>Điểm mạnh & điều cần cân nhắc</summary>
              <ul>{card.strengths.map((text) => <li key={text}>{text}</li>)}</ul>
              <ul>{card.concerns.map((text) => <li key={text}>{text}</li>)}</ul>
            </details>
          </>}
          {card.expiresAt && card.status !== "MATCHED" && <p className="fineprint">Chờ phản hồi đến {new Date(card.expiresAt).toLocaleString("vi-VN")}</p>}
          {saved && <p role="status">{card.status === "MATCHED" ? "Hai bên đã Match. Mở hồ sơ để xác nhận kết nối và trao đổi liên hệ." : "Bạn đã OK. Đang chờ bên kia phản hồi."}</p>}
          {saved && card.activityAt && <p className="fineprint">{card.status === "MATCHED" ? "Match lúc" : "Bạn đã OK lúc"} {new Date(card.activityAt).toLocaleString("vi-VN")}</p>}
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
      </>}
    </div>
  );
}
