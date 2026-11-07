import { useState } from "react";
import { Avatar, Field, Notice } from "../../components/shared";

export function TrialForm({ tutor, student, send, busy }) {
  const [openedAt] = useState(() => Date.now());
  const localDate = (date) => {
    const local = new Date(date);
    local.setMinutes(local.getMinutes() - local.getTimezoneOffset());
    return local.toISOString().slice(0, 16);
  };
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
          <input
            name="date"
            type="datetime-local"
            required
            min={localDate(openedAt + 61 * 60000)}
            max={localDate(openedAt + 90 * 86400000)}
          />
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
