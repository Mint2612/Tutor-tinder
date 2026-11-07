import { useWorkspace } from "../hooks/useWorkspace";
import { api } from "../services/api";
import { statusText } from "../lib/constants";
import { PageHeading, Empty } from "../components/shared";

export function LessonsPage() {
  const { user, busy, lessons, setLessons, run, navigate } = useWorkspace();
  return (
    <>
      <PageHeading
        eyebrow="MỖI KẾT NỐI, MỘT CƠ HỘI"
        title="Lịch học thử"
        description="Thông tin liên hệ chỉ được mở sau khi gia sư chấp nhận."
      />
      <button
        className="secondary"
        disabled={busy}
        onClick={() => navigate("lessons")}
      >
        Cập nhật trạng thái
      </button>
      <div className="lesson-list">
        {lessons.map((l) => (
          <article className="panel lesson" key={l.id}>
            <div className="row">
              <div>
                <h3>
                  {l.nickname} <span className="muted">&</span> {l.display_name}
                </h3>
                <p>{new Date(l.proposed_at).toLocaleString("vi-VN")}</p>
              </div>
              <span className={`status ${l.status.toLowerCase()}`}>
                {statusText[l.status]}
              </span>
            </div>
            <p>{l.message || "Không có lời nhắn"}</p>
            {l.status === "ACCEPTED" && (
              <div className="contact">
                <strong>Đã kết nối · Liên hệ để thống nhất buổi học</strong>
                <p>
                  Phụ huynh: {l.parent_email} · {l.parent_phone}
                </p>
                <p>
                  Gia sư: {l.tutor_email} · {l.tutor_phone}
                </p>
              </div>
            )}
            {user.role === "TUTOR" && l.status === "PENDING" && (
              <div className="row">
                <button
                  disabled={busy}
                  onClick={() =>
                    run(async () => {
                      await api(`/lessons/${l.id}`, {
                        method: "PUT",
                        body: { status: "ACCEPTED" },
                      });
                      setLessons(await api("/lessons"));
                    })
                  }
                >
                  Chấp nhận
                </button>
                <button
                  className="secondary"
                  disabled={busy}
                  onClick={() =>
                    run(async () => {
                      await api(`/lessons/${l.id}`, {
                        method: "PUT",
                        body: { status: "REJECTED" },
                      });
                      setLessons(await api("/lessons"));
                    })
                  }
                >
                  Từ chối
                </button>
              </div>
            )}
          </article>
        ))}
      </div>
      {!lessons.length && (
        <Empty
          title="Chưa có yêu cầu học thử"
          text="Các lời mời và phản hồi sẽ xuất hiện tại đây."
        />
      )}
    </>
  );
}
