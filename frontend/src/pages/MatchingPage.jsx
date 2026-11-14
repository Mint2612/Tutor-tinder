import { useEffect, useState } from "react";
import { useWorkspace } from "../hooks/useWorkspace";
import { api } from "../services/api";
import { PageHeading, Empty, StudentPicker } from "../components/shared";
import { MatchingCard } from "../features/discovery/MatchingCard";
import { MatchDetail } from "../features/discovery/MatchDetail";
import { AiSuggestions } from "../features/discovery/AiSuggestions";

export function MatchingPage({ saved = false }) {
  const { user, students, selected, setSelected, busy, run, setNotification, navigate } = useWorkspace();
  const [refresh, setRefresh] = useState(0);
  const [snapshot, setSnapshot] = useState(null);
  const [detail, setDetail] = useState(null);
  const parent = user.role === "PARENT";
  const key = `${user.id}:${selected}:${refresh}`;
  const hasProfile = !parent || !!selected;
  const loading = hasProfile && snapshot?.key !== key;
  const feed = snapshot?.key === key ? snapshot.feed : null;
  const error = snapshot?.key === key ? snapshot.error : null;
  useEffect(() => {
    if (!hasProfile) return;
    const controller = new AbortController();
    api(parent ? `/students/${selected}/matching` : "/tutor/matching", { signal: controller.signal })
      .then((feed) => setSnapshot({ key, feed }))
      .catch((e) => { if (e.name !== "AbortError") setSnapshot({ key, error: e.message }); });
    return () => controller.abort();
  }, [hasProfile, key, parent, selected]);
  useEffect(() => {
    const timer = setInterval(() => setRefresh((n) => n + 1), 30000);
    return () => clearInterval(timer);
  }, []);
  const cards = saved ? feed?.savedCards : feed?.cards;
  const choose = (card, interested) => run(async () => {
    try {
      const result = await api("/matching/actions", { method: "POST", body: { studentId: card.studentId, tutorId: card.tutorId, interested } });
      setNotification(result.message);
    } finally {
      setRefresh((n) => n + 1);
    }
  });
  return <>
    <PageHeading eyebrow="HAI BÊN CÙNG LỰA CHỌN"
      title={saved ? "Đã OK & Match" : parent ? "Gặp người đồng hành của con." : "Tìm học sinh phù hợp."}
      description="Chỉ tạo Match khi cả hai cùng OK. Bỏ qua đóng cặp cho cả hai bên, không thông báo lý do." />
    <div className="toolbar">
      {parent && !saved && selected && (
        <AiSuggestions key={selected} studentId={selected} run={run} busy={busy}
          onChanged={() => setRefresh((n) => n + 1)} />
      )}
      {parent && <StudentPicker disabled={busy || loading} students={students} selected={selected} onChange={setSelected} />}
      <button className="secondary" disabled={busy || loading} onClick={() => setRefresh((n) => n + 1)}>Cập nhật danh sách</button>
    </div>
    {!hasProfile && <Empty title="Hãy tạo hồ sơ học sinh trước" text="Hoàn thành hồ sơ và khảo sát để nhận danh sách riêng cho con." />}
    {loading && <p role="status">Đang tải danh sách phù hợp…</p>}
    {error && <div className="error" role="alert">{error} <button onClick={() => setRefresh((n) => n + 1)}>Thử lại</button></div>}
    {feed && <>
      {feed.paused && <div className="notice" role="status">Hồ sơ đang tạm ẩn. Đề xuất mới được tạm dừng; Match đã có vẫn được giữ. <button className="secondary" onClick={() => navigate(parent ? "students" : "profile")}>Quản lý hiển thị</button></div>}
      <p className="matching-limits">Còn {feed.limits.totalRemaining}/10 lượt OK hôm nay · {feed.limits.incomingRemaining}/3 lượt OK vào thẻ “Quan tâm bạn”. Mỗi hồ sơ có giới hạn riêng; đặt lại lúc 00:00 giờ Việt Nam.</p>
      {!cards?.length && <Empty title={saved ? "Chưa có lựa chọn đang chờ hoặc Match" : "Chưa có hồ sơ phù hợp"}
        text={saved ? "Lựa chọn OK được lưu tại đây và giữ qua lần tải lại. Cặp bị bỏ qua hoặc hết hạn sẽ đóng." : "Danh sách chỉ gồm hồ sơ đáp ứng môn, lớp, hình thức, lịch và ngân sách. Khi học trực tiếp, hai bên cần cùng tỉnh. Bạn có thể cập nhật hồ sơ hoặc quay lại sau."} />}
      {saved ? <div className="matching-saved">{cards.map((card) => <MatchingCard key={`${card.studentId}:${card.tutorId}`} card={card} saved busy={busy || loading} onDetail={() => setDetail(card)} />)}</div>
        : cards?.[0] && <div className="matching-deck"><p>{cards.length} thẻ trong lượt này · Vuốt phải để OK, vuốt trái để Bỏ qua.</p>
          <MatchingCard key={`${cards[0].studentId}:${cards[0].tutorId}`} card={cards[0]} busy={busy || loading} onChoose={(like) => choose(cards[0], like)} /></div>}
    </>}
    {detail && (
      <MatchDetail card={detail} run={run} busy={busy}
        close={() => { setDetail(null); setRefresh((n) => n + 1); }} />
    )}
  </>;
}