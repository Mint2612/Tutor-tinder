import { useWorkspace } from "../hooks/useWorkspace";
import { api } from "../services/api";
import { statusText } from "../lib/constants";
import { PageHeading, Empty } from "../components/shared";

export function AdminPage() {
  const { busy, queue, setReview, setQueue, run } = useWorkspace();
  // Hồ sơ đang chờ duyệt lên đầu để admin không phải tìm trong cả danh sách.
  const pendingFirst = (t) => (t.workflow_state === "PENDING_REVIEW" ? 1 : 0);
  const waiting = queue.filter((t) => pendingFirst(t)).length;
  const sorted = [...queue].sort((a, b) => pendingFirst(b) - pendingFirst(a));
  return (
    <>
      <PageHeading
        eyebrow="QUẢN TRỊ & TIN CẬY"
        title="Xét duyệt gia sư"
        description={`Đọc bằng chứng danh tính và đánh giá hồ sơ trước khi phê duyệt. Đang chờ duyệt: ${waiting} hồ sơ.`}
      />
      <div className="grid">
        {sorted.map((t) => (
          <article className="panel" key={t.id}>
            <h3>{t.display_name || "Hồ sơ chưa hoàn thiện"}</h3>
            <span className="status">{statusText[t.workflow_state]}</span>
            <p>Họ và tên khai báo: {t.full_name || "chưa khai"}</p>
            <p>Hiển thị: {t.visible ? "Bật" : "Tắt"}</p>
            <button
              className="secondary"
              disabled={busy}
              onClick={() =>
                run(async () => setReview(await api(`/admin/tutors/${t.id}`)))
              }
            >
              Xem hồ sơ & giấy tờ
            </button>
            <button
              className="text-button"
              disabled={busy}
              onClick={() =>
                run(async () => {
                  await api(`/admin/tutors/${t.id}/visibility`, {
                    method: "PUT",
                    body: { visible: !t.visible },
                  });
                  setQueue(await api("/admin/tutors"));
                })
              }
            >
              {t.visible ? "Ẩn gia sư" : "Bật hiển thị"}
            </button>
          </article>
        ))}
      </div>
      {!queue.length && (
        <Empty
          title="Chưa có hồ sơ gia sư"
          text="Hồ sơ mới sẽ hiển thị ở đây."
        />
      )}
    </>
  );
}
