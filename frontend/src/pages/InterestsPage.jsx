import { useWorkspace } from "../hooks/useWorkspace";
import { api } from "../services/api";
import {
  Avatar,
  Empty,
  PageHeading,
  StudentPicker,
} from "../components/shared";
import { money } from "../lib/constants";

export function InterestsPage() {
  const {
    students,
    selected,
    interests,
    busy,
    loading,
    setSelected,
    setInterests,
    setInterest,
    setDetail,
    run,
    navigate,
  } = useWorkspace();
  return (
    <>
      <PageHeading
        eyebrow="LƯU LẠI NHỮNG KẾT NỐI"
        title="Gia sư đã quan tâm"
        description="Mở lại hồ sơ đã thích và gửi yêu cầu học thử khi gia đình sẵn sàng."
      />
      <div className="toolbar">
        <StudentPicker
          disabled={busy}
          students={students}
          selected={selected}
          onChange={setSelected}
        />
        <button
          className="secondary"
          disabled={busy}
          onClick={() => navigate("interests")}
        >
          Cập nhật danh sách
        </button>
      </div>
      {!loading && !interests.length && (
        <Empty
          title="Chưa có gia sư đã quan tâm phù hợp"
          text="Các gia sư bạn thích sẽ xuất hiện ở đây khi hồ sơ còn đáp ứng điều kiện học của con."
        />
      )}
      {!loading && (
        <div className="grid">
          {interests.map((t) => (
            <article className="panel" key={t.id}>
              <Avatar name={t.display_name} photo={t.photo_url} />
              <h2>{t.display_name}</h2>
              <span className="verified">✓ Đã xác minh</span>
              <p>
                {t.subjects.join(" · ")} · {money(t.hourly_rate)}/giờ
              </p>
              <p className="muted">{t.teaching_style}</p>
              <div className="row">
                <button disabled={busy} onClick={() => setInterest(t)}>
                  Gửi yêu cầu học thử
                </button>
                <button
                  className="secondary"
                  disabled={busy}
                  onClick={() =>
                    run(async () => setDetail(await api(`/tutors/${t.id}`)))
                  }
                >
                  Xem hồ sơ
                </button>
              </div>
              <button
                className="text-button"
                disabled={busy}
                onClick={() =>
                  run(async () => {
                    await api(`/students/${selected}/interests/${t.id}`, {
                      method: "DELETE",
                    });
                    setInterests((list) =>
                      list.filter((item) => item.id !== t.id),
                    );
                  })
                }
              >
                Bỏ quan tâm
              </button>
            </article>
          ))}
        </div>
      )}
    </>
  );
}
