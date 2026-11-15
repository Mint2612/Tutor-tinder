import { useEffect, useState } from "react";
import { api } from "../services/api";
import { PageHeading, Empty } from "../components/shared";
import { TrialScheduler } from "../features/lessons/TrialScheduler";
import { useWorkspace } from "../hooks/useWorkspace";

export function LessonsPage() {
  const { user, navigate } = useWorkspace();
  const [items, setItems] = useState(null);
  const [error, setError] = useState("");
  const [refresh, setRefresh] = useState(0);
  const [pair, setPair] = useState(null);
  useEffect(() => {
    const controller = new AbortController();
    api("/trials", { signal: controller.signal }).then(setItems).catch((e) => {
      if (e.name !== "AbortError") setError(e.message);
    });
    return () => controller.abort();
  }, [refresh]);
  const pairs = [...new Map(items?.map((t) => [`${t.studentId}:${t.tutorId}`, t]) || []).values()];
  return <>
    <PageHeading eyebrow="THỐNG NHẤT BUỔI HỌC ĐẦU TIÊN" title="Lịch học thử" description="Sau khi hai bên xác nhận kết nối, đề xuất giờ và chốt lịch tại hồ sơ Match." />
    <button className="secondary" onClick={() => { setPair(null); setItems(null); setError(""); setRefresh((n) => n + 1); }}>Cập nhật trạng thái</button>
    {user.role === "PARENT" && <button className="text-button" onClick={() => navigate("reviews")}>Đánh giá buổi học đã tham gia</button>}
    {error && <div className="error" role="alert">{error} <button onClick={() => { setError(""); setRefresh((n) => n + 1); }}>Thử lại</button></div>}
    {!items && !error && <p role="status">Đang tải lịch học thử…</p>}
    {items && !items.length && <Empty title="Chưa có yêu cầu học thử" text="Mở Đã OK & Match, xem hồ sơ và xác nhận kết nối để đề xuất lịch." />}
    {pair ? <TrialScheduler key={`${pair.studentId}:${pair.tutorId}`} studentId={pair.studentId} tutorId={pair.tutorId} />
      : <div className="lesson-list">{pairs.map((t, i) => <article className="panel" key={`${t.studentId}:${t.tutorId}`}>
        <h2>Buổi học thử {i + 1}</h2>
        <button onClick={() => setPair(t)}>Quản lý lịch học thử</button>
      </article>)}</div>}
  </>;
}
