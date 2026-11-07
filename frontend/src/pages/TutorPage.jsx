import { useWorkspace } from "../hooks/useWorkspace";
import { api } from "../services/api";
import { statusText } from "../lib/constants";
import { PageHeading, Field } from "../components/shared";
import { TutorForm } from "../features/tutors/TutorForm";

export function TutorPage() {
  const {
    busy,
    tutor,
    documents,
    setNotification,
    setTutor,
    setDocuments,
    run,
  } = useWorkspace();
  return (
    <>
      <PageHeading
        eyebrow="KHÔNG GIAN GIA SƯ"
        title="Chia sẻ cách bạn giúp học sinh tiến bộ."
        description="Hoàn thiện hồ sơ, gửi bằng chứng danh tính và chờ quản trị viên duyệt."
      />
      {tutor && (
        <div className="panel row">
          <span className="status">{statusText[tutor.workflow_state]}</span>
          <span>{tutor.rejection_reason}</span>
        </div>
      )}
      <TutorForm
        key={tutor?.revision ?? "new"}
        initial={tutor}
        busy={busy}
        save={(form) =>
          run(async () => {
            setTutor(
              await api("/tutor/profile", { method: "PUT", body: form }),
            );
            setNotification(
              "Đã lưu bản nháp. Hồ sơ cần được duyệt lại trước khi xuất hiện trong gợi ý.",
            );
          })
        }
      />
      {tutor && (
        <section className="panel">
          <h2>Giấy tờ xác minh riêng tư</h2>
          <p className="muted">
            Bằng chứng danh tính: PDF, PNG, JPEG, tối đa 5 MB mỗi tệp, 5 tệp.
            Chỉ quản trị viên có quyền đọc.
          </p>
          <ul>
            {documents.map((d) => (
              <li key={d.id}>
                {d.filename}{" "}
                {["DRAFT", "REJECTED"].includes(tutor.workflow_state) && (
                  <button
                    className="text-button"
                    disabled={busy}
                    onClick={() =>
                      run(async () => {
                        await api(`/tutor/documents/${d.id}`, {
                          method: "DELETE",
                        });
                        setDocuments(await api("/tutor/documents"));
                      })
                    }
                  >
                    Xóa
                  </button>
                )}
              </li>
            ))}
          </ul>
          {["DRAFT", "REJECTED"].includes(tutor.workflow_state) && (
            <>
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
                      await api("/tutor/documents", {
                        method: "POST",
                        body: form,
                      });
                      setDocuments(await api("/tutor/documents"));
                    });
                    e.target.value = "";
                  }}
                />
              </Field>
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
            </>
          )}
        </section>
      )}
    </>
  );
}
