import { useRef, useState } from "react";
import { Avatar, Availability, Field, Notice } from "./shared";
import { money, levelText, statusText } from "../lib/constants";

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
          if (busy || e.target.closest("button")) return;
          start.current = e.clientX;
          e.currentTarget.setPointerCapture(e.pointerId);
        }}
        onPointerMove={(e) => {
          if (start.current !== null)
            setOffset(Math.max(-140, Math.min(140, e.clientX - start.current)));
        }}
        onPointerUp={() => {
          if (start.current === null) return;
          const direction = offset;
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

export function TutorDetails({ tutor: t }) {
  return (
    <>
      <Avatar name={t.display_name} photo={t.photo_url} large />
      <h2>{t.display_name}</h2>
      <span className="status">{statusText[t.workflow_state]}</span>
      <p>
        {t.subjects.join(" · ")} · Lớp {t.grades.join(", ")}
      </p>
      <p>
        {money(t.hourly_rate)}/giờ · {t.province}
      </p>
      <h3>Học vấn & kinh nghiệm</h3>
      <p>
        {t.background} · {t.experience_years} năm kinh nghiệm
      </p>
      <h3>Cách tôi giảng dạy</h3>
      <p>{t.teaching_style}</p>
      <p>{t.teaching_description}</p>
      <h3>Lịch có thể sắp xếp</h3>
      <p>
        <Availability slots={t.availability} />
      </p>
    </>
  );
}

export function TrialForm({ tutor, student, send, busy }) {
  return (
    <>
      <div className="match-avatars">
        <Avatar name={student?.nickname} />
        <span>✦</span>
        <Avatar name={tutor.display_name} photo={tutor.photo_url} />
      </div>
      <p className="eyebrow">MỘT KHỞI ĐẦU ĐÁNG MONG ĐỢI</p>
      <h2>Bạn đã tìm thấy một kết nối tiềm năng!</h2>
      <p>
        {student?.nickname} & {tutor.display_name}
      </p>
      <Notice>
        Đây là sự quan tâm từ gia đình. Kết nối chỉ được xác nhận sau khi gia sư
        chấp nhận yêu cầu học thử.
      </Notice>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          const data = Object.fromEntries(new FormData(e.currentTarget));
          send({
            proposedAt: new Date(data.date).toISOString(),
            message: data.message,
          });
        }}
      >
        <Field label="Ngày và giờ đề xuất (giờ địa phương)">
          <input name="date" type="datetime-local" required />
        </Field>
        <Field label="Lời nhắn cho gia sư (tùy chọn)">
          <textarea
            name="message"
            maxLength={2000}
            rows={3}
            placeholder="Chia sẻ mong muốn cho buổi học đầu tiên…"
          />
        </Field>
        <button className="full" disabled={busy || !student}>
          Gửi yêu cầu học thử →
        </button>
      </form>
    </>
  );
}

export function AdminReview({ tutor, decide, busy, run }) {
  const [opened, setOpened] = useState(false);
  return (
    <>
      <TutorDetails tutor={tutor} />
      <h3>Bằng chứng danh tính riêng tư</h3>
      {tutor.documents.map((d) => (
        <p key={d.id}>
          <button
            className="secondary"
            disabled={busy}
            onClick={() =>
              run(async () => {
                const r = await fetch(`/api/admin/documents/${d.id}`, {
                  credentials: "same-origin",
                });
                if (!r.ok) throw new Error("Không thể đọc giấy tờ");
                const blob = await r.blob();
                const url = URL.createObjectURL(blob);
                const a = document.createElement("a");
                a.href = url;
                a.download = d.filename;
                a.click();
                setTimeout(() => URL.revokeObjectURL(url), 30000);
                setOpened(true);
              })
            }
          >
            Tải & xem {d.filename}
          </button>
        </p>
      ))}
      {tutor.workflow_state === "PENDING_REVIEW" && (
        <form
          onSubmit={(e) => {
            e.preventDefault();
            const reason = new FormData(e.currentTarget).get("reason");
            decide({
              approve: e.nativeEvent.submitter.value === "approve",
              reason,
            });
          }}
        >
          <Field label="Lý do / ghi chú (bắt buộc nếu từ chối)">
            <textarea name="reason" maxLength={1000} rows={3} />
          </Field>
          <div className="row">
            <button value="approve" disabled={busy || !opened}>
              Phê duyệt sau khi kiểm tra
            </button>
            <button value="reject" className="secondary" disabled={busy}>
              Từ chối
            </button>
          </div>
        </form>
      )}
    </>
  );
}
