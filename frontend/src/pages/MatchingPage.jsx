import { useEffect, useState } from "react";
import { useWorkspace } from "../hooks/useWorkspace";
import { api } from "../services/api";
import { PageHeading, Empty, StudentPicker } from "../components/shared";
import { MatchingCard } from "../features/discovery/MatchingCard";
import { MatchDetail } from "../features/discovery/MatchDetail";
import { AiCarousel, AI_SUGGESTION_COUNT } from "../features/discovery/AiCarousel";

export function MatchingPage({ saved = false }) {
  const { user, students, selected, setSelected, busy, run, setNotification, navigate } = useWorkspace();
  const [refresh, setRefresh] = useState(0);
  const [snapshot, setSnapshot] = useState(null);
  const [detail, setDetail] = useState(null);
  const [aiMode, setAiMode] = useState(false);
  const parent = user.role === "PARENT";
  const key = `${user.id}:${selected}:${refresh}:${aiMode}`;
  const hasProfile = !parent || !!selected;
  const loading = hasProfile && snapshot?.key !== key;
  const feed = snapshot?.key === key ? snapshot.feed : null;
  const error = snapshot?.key === key ? snapshot.error : null;
  useEffect(() => {
    if (!hasProfile) return;
    const controller = new AbortController();
    const path = parent ? `/students/${selected}/matching` : "/tutor/matching";
    const load = async () => {
      if (aiMode && !saved) {
        const cards = await api(`${path}/ai`, { signal: controller.signal });
        const result = await api(path, { signal: controller.signal });
        return { ...result, cards };
      }
      return api(path, { signal: controller.signal });
    };
    load()
      .then((feed) => setSnapshot({ key, feed }))
      .catch((e) => { if (e.name !== "AbortError") setSnapshot({ key, error: e.message }); });
    return () => controller.abort();
  }, [hasProfile, key, parent, selected, aiMode, saved]);
  useEffect(() => {
    if (aiMode && !saved) return;
    const timer = setInterval(() => setRefresh((n) => n + 1), 30000);
    return () => clearInterval(timer);
  }, [aiMode, saved]);
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
      title={saved ? "Đã OK & Match" : parent ? "Gặp người đồng hành của bạn." : "Tìm học sinh phù hợp."}
      description="Chỉ tạo Match khi cả hai cùng OK. Bỏ qua đóng cặp cho cả hai bên, không thông báo lý do." />
    <div className={`toolbar matching-toolbar${parent && !saved ? " discovery-toolbar" : ""}`}>
      {!saved && hasProfile && <button className="secondary" disabled={busy || loading} aria-pressed={aiMode}
        onClick={() => setAiMode((value) => !value)}>{aiMode ? "Xem gợi ý theo luật" : `Gợi ý ${AI_SUGGESTION_COUNT} hồ sơ bằng AI`}</button>}
      {parent && <StudentPicker disabled={busy || loading} students={students} selected={selected} onChange={setSelected} />}
      <button className="secondary" disabled={busy || loading} onClick={() => setRefresh((n) => n + 1)}>Cập nhật danh sách</button>
    </div>
    {!hasProfile && <Empty title="Hãy tạo hồ sơ học sinh trước" text="Hoàn thành hồ sơ và khảo sát để nhận danh sách riêng cho con." />}
    {loading && <p role="status">Đang tải danh sách phù hợp…</p>}
    {/* {aiMode && !saved && <p className="fineprint">AI so sánh các thuộc tính học tập đã ẩn danh trong tối đa 20 hồ sơ đủ điều kiện; kết quả tham khảo. Khi AI không khả dụng, hệ thống dùng luật.</p>} */}
    {error && <div className="error" role="alert">{error} <button onClick={() => setRefresh((n) => n + 1)}>Thử lại</button></div>}
    {feed && <>
      {feed.paused && <div className="notice" role="status">Hồ sơ đang tạm ẩn. Đề xuất mới được tạm dừng; Match đã có vẫn được giữ. <button className="secondary" onClick={() => navigate(parent ? "students" : "profile")}>Quản lý hiển thị</button></div>}
      {!cards?.length && <Empty title={saved ? "Chưa có lựa chọn đang chờ hoặc Match" : "Chưa có hồ sơ phù hợp"}
        text={saved ? "Lựa chọn OK được lưu tại đây và giữ qua lần tải lại. Cặp bị bỏ qua hoặc hết hạn sẽ đóng." : "Danh sách chỉ gồm hồ sơ đáp ứng môn, lớp, hình thức, lịch và ngân sách. Khi học trực tiếp, hai bên cần cùng tỉnh. Bạn có thể cập nhật hồ sơ hoặc quay lại sau."} />}
      {saved && !!cards?.length && <p className="fineprint">Sắp xếp theo thời gian: mới nhất ở trên cùng.</p>}
      {saved ? <div className="matching-saved">{cards.map((card) => <MatchingCard key={`${card.studentId}:${card.tutorId}`} card={card} saved busy={busy || loading} onDetail={() => setDetail(card)} />)}</div>
        : aiMode && cards?.length ? <AiCarousel key={cards.slice(0, AI_SUGGESTION_COUNT).map((card) => `${card.studentId}:${card.tutorId}`).join(",")}
          cards={cards} busy={busy || loading} onChoose={choose} />
        : cards?.[0] && <div className="matching-deck"><p>{cards.length} thẻ trong lượt này · Vuốt phải để OK, vuốt trái để Bỏ qua.</p>
          <MatchingCard key={`${cards[0].studentId}:${cards[0].tutorId}`} card={cards[0]} busy={busy || loading} onChoose={(like) => choose(cards[0], like)} /></div>}
    </>}
    {detail && (
      <MatchDetail card={detail} run={run} busy={busy}
        close={() => { setDetail(null); setRefresh((n) => n + 1); }} />
    )}
  </>;
}
