import { useState } from "react";
import { useWorkspace } from "../../hooks/useWorkspace";
import { api } from "../../services/api";
import { Field } from "../../components/shared";

// Identity/degree evidence: the only part of a tutor's profile an admin reviews. Files live in
// private object storage; only an admin can read them, and only through the API.
const ACCEPT = "application/pdf,image/png,image/jpeg";
const reviewNote = {
  APPROVED:
    "Hồ sơ đã được duyệt. Thêm, thay, đổi tên hoặc xóa minh chứng sẽ đưa hồ sơ về bản nháp để duyệt lại; thông tin và ảnh thì sửa tự do.",
  PENDING_REVIEW:
    "Hồ sơ đang chờ duyệt. Nếu thêm, thay, đổi tên hoặc xóa minh chứng, bạn cần gửi duyệt lại.",
};

function describe(doc) {
  const kind = doc.content_type === "application/pdf" ? "PDF" : "Ảnh";
  const date = new Date(doc.created_at);
  return Number.isNaN(date.getTime())
    ? kind
    : `${kind} · tải lên ${date.toLocaleDateString("vi-VN")}`;
}

// Rename a document, swap its file, or both. Nothing is sent unless something really changed.
function DocumentEditor({ doc, busy, onSave, onCancel }) {
  const [name, setName] = useState(doc.filename);
  const [file, setFile] = useState(null);
  const trimmed = name.trim();
  const changed = file !== null || trimmed !== doc.filename;
  return (
    <form
      className="evidence-edit"
      onSubmit={(e) => {
        e.preventDefault();
        onSave({ name: trimmed, file });
      }}
    >
      <Field label="Tên giấy tờ">
        <input
          required
          maxLength={120}
          value={name}
          onChange={(e) => setName(e.target.value)}
        />
      </Field>
      <Field label="Thay bằng tệp khác (tùy chọn)">
        <input
          type="file"
          accept={ACCEPT}
          onChange={(e) => setFile(e.target.files[0] ?? null)}
        />
      </Field>
      <div className="evidence-actions">
        <button className="small" disabled={busy || !changed || !trimmed}>
          Lưu thay đổi
        </button>
        <button
          type="button"
          className="secondary small"
          disabled={busy}
          onClick={onCancel}
        >
          Hủy
        </button>
      </div>
    </form>
  );
}

export function EvidenceSection() {
  const { busy, tutor, documents, setTutor, setDocuments, setNotification, run } =
    useWorkspace();
  const [editing, setEditing] = useState(null);
  const canSubmit = ["DRAFT", "REJECTED"].includes(tutor.workflow_state);
  // A change can withdraw the current decision, so the profile status has to be reloaded too.
  const refresh = async () => {
    setDocuments(await api("/tutor/documents"));
    setTutor(await api("/tutor/profile"));
  };
  const save = async (doc, { name, file }) => {
    const done = await run(async () => {
      const form = new FormData();
      form.append("name", name);
      if (file) form.append("file", file);
      await api(`/tutor/documents/${doc.id}`, { method: "PUT", body: form });
      await refresh();
    });
    if (done) {
      setEditing(null);
      setNotification("Đã cập nhật giấy tờ.");
    }
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
      <ul className="evidence-list">
        {documents.map((d) => (
          <li key={d.id}>
            {editing === d.id ? (
              <DocumentEditor
                doc={d}
                busy={busy}
                onSave={(change) => save(d, change)}
                onCancel={() => setEditing(null)}
              />
            ) : (
              <>
                <div className="evidence-name">
                  <strong>{d.filename}</strong>
                  <span className="muted">{describe(d)}</span>
                </div>
                <div className="evidence-actions">
                  <button
                    className="text-button"
                    disabled={busy}
                    onClick={() => setEditing(d.id)}
                  >
                    Sửa
                  </button>
                  <button
                    className="text-button"
                    disabled={busy}
                    onClick={() =>
                      run(async () => {
                        await api(`/tutor/documents/${d.id}`, {
                          method: "DELETE",
                        });
                        await refresh();
                      })
                    }
                  >
                    Xóa
                  </button>
                </div>
              </>
            )}
          </li>
        ))}
      </ul>
      <Field label="Tải bằng chứng danh tính">
        <input
          type="file"
          accept={ACCEPT}
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
