import { useState } from "react";
import { Field } from "../../components/shared";
import { statusText } from "../../lib/constants";
import { TutorDetails } from "../tutors/TutorDetails";

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
      {!!tutor.audit?.length && (
        <section>
          <h3>Lịch sử xét duyệt</h3>
          <ul>
            {tutor.audit.map((entry) => (
              <li key={entry.id}>
                {new Date(entry.created_at).toLocaleString("vi-VN")} ·{" "}
                {statusText[entry.decision]}
                {entry.reason ? `: ${entry.reason}` : ""}
                {entry.submission_id && ` · Lần nộp ${entry.submission_id} · Phiên bản ${entry.revision}`}
              </li>
            ))}
          </ul>
        </section>
      )}
    </>
  );
}
