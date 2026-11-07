import { useWorkspace } from "../hooks/useWorkspace";
import { api } from "../services/api";
import { PageHeading, Empty, StudentPicker } from "../components/shared";
import { SwipeCard } from "../features/discovery/SwipeCard";

export function DiscoveryPage() {
  const {
    busy,
    students,
    selected,
    recommendations,
    loadedDiscovery,
    setSelected,
    setRecommendations,
    setLoadedDiscovery,
    setDetail,
    run,
    discover,
    swipe,
  } = useWorkspace();
  return (
    <>
      <PageHeading
        eyebrow="MỘT NGƯỜI THẦY. NHIỀU KHỞI ĐẦU."
        title="Gặp người đồng hành của con."
        description="Lướt để tìm hiểu. Quyền quyết định luôn ở gia đình bạn."
      />
      <div className="toolbar">
        <StudentPicker
          disabled={busy}
          students={students}
          selected={selected}
          onChange={(id) => {
            setSelected(id);
            setRecommendations([]);
            setLoadedDiscovery(false);
          }}
        />
        <button
          className="secondary"
          disabled={busy || !selected}
          onClick={discover}
        >
          Lấy gợi ý mới
        </button>
      </div>
      {recommendations.length ? (
        <div className="discovery">
          <aside className="discovery-guide">
            <span className="eyebrow">
              DÀNH CHO {students.find((s) => s.id === selected)?.nickname}
            </span>
            <h2>
              Phù hợp bắt đầu
              <br />
              từ thấu hiểu.
            </h2>
            <p>
              Chỉ những gia sư đáp ứng môn học, lịch, hình thức và ngân sách của
              con mới xuất hiện ở đây.
            </p>
            <div className="guide-point">✓ Đã được xác minh danh tính</div>
            <div className="guide-point">✦ Gợi ý có lý do rõ ràng</div>
            <p className="fineprint">
              Mức độ phù hợp là nhận định tham khảo, không phải xác suất khoa
              học. Học thử là cơ hội để hai bên tìm hiểu.
            </p>
          </aside>
          <SwipeCard
            key={recommendations[0].tutor.id}
            recommendation={recommendations[0]}
            busy={busy}
            onSwipe={swipe}
            onDetail={() =>
              run(async () =>
                setDetail(await api(`/tutors/${recommendations[0].tutor.id}`)),
              )
            }
          />
          <aside className="discovery-side">
            <span className="count">
              {recommendations.length.toString().padStart(2, "0")}
            </span>
            <p>hồ sơ để khám phá</p>
            <hr />
            <p>
              Kéo sang phải để quan tâm.
              <br />
              Kéo sang trái để bỏ qua.
            </p>
            <p className="fineprint">
              Quan tâm không đồng nghĩa gia sư đã nhận lời. Bạn cần gửi yêu cầu
              học thử.
            </p>
          </aside>
        </div>
      ) : (
        <Empty
          title={
            loadedDiscovery
              ? "Bạn đã xem hết gợi ý hiện tại"
              : "Sẵn sàng gặp gia sư phù hợp?"
          }
          text={
            loadedDiscovery
              ? "Chưa có thêm hồ sơ đã xác minh đáp ứng đầy đủ điều kiện. Bạn có thể điều chỉnh hồ sơ hoặc quay lại sau."
              : "Chọn hồ sơ của con và nhấn “Lấy gợi ý mới” để bắt đầu."
          }
        />
      )}
    </>
  );
}
