import { useWorkspace } from "../../hooks/useWorkspace";
import { api } from "../../services/api";
import { Field } from "../../components/shared";

// Identity/degree evidence: the only part of a tutor's profile an admin reviews. Files live in
// private object storage; only an admin can read them, and only through the API.
const reviewNote = {
  APPROVED:
    "Hồ sơ đã được duyệt. Thêm hoặc xóa minh chứng sẽ đưa hồ sơ về bản nháp để duyệt lại; thông tin và ảnh thì sửa tự do.",
  PENDING_REVIEW:
    "Hồ sơ đang chờ duyệt. Nếu thêm hoặc xóa minh chứng, bạn cần gửi duyệt lại.",
};

export function EvidenceSection() {
  const { busy, tutor, documents, setTutor, setDocuments, setNotification, run } =
    useWorkspace();
  const canSubmit = ["DRAFT", "REJECTED"].includes(tutor.workflow_state);
  // A change can withdraw the current decision, so the profile status has to be reloaded too.
  const refresh = async () => {
    setDocuments(await api("/tutor/documents"));
    setTutor(await api("/tutor/profile"));
  };
  return (
    <section className="panel">
      <h2>Giấy tờ xác minh riêng tư</h2>
      <p className="muted">
        Bằng chứng danh tính: PDF, PNG, JPEG, tối đa 5 MB mỗi tệp, 5 tệp. Tệp
        được lưu trong kho riêng tư và chỉ quản trị viên có quyền đọc.
      </p>
      {reviewNote[tutor.workflow_state] && (
        <p className="notice">{reviewNote[tutor.workflow_state]}</p>
      )}
      <ul>
        {documents.map((d) => (
          <li key={d.id}>
            {d.filename}{" "}
            <button
              className="text-button"
              disabled={busy}
              onClick={() =>
                run(async () => {
                  await api(`/tutor/documents/${d.id}`, { method: "DELETE" });
                  await refresh();
                })
              }
            >
              Xóa
            </button>
          </li>
        ))}
      </ul>
      <Field label="Tải bằng chứng danh tính">
        <input
          type="file"
          accept="application/pdf,image/png,image/jpeg"
          disabled={busy}
          onChange={(e) => {
            const file = e.target.files[0];
            if (!file) return;
            run(async () => {
              const form = new FormData();
              form.append("file", file);
              await api("/tutor/documents", { method: "POST", body: form });
              await refresh();
            });
            e.target.value = "";
          }}
        />
      </Field>
      {canSubmit && (
        <button
          disabled={busy || !documents.length}
          onClick={() =>
            run(async () => {
              setTutor(await api("/tutor/submit", { method: "POST" }));
              setNotification("Hồ sơ đã được gửi để xét duyệt.");
            })
          }
        >
          Gửi hồ sơ để duyệt
        </button>
      )}
    </section>
  );
}
