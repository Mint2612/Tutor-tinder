import { useEffect, useRef, useState } from "react";
import { api } from "../services/api";
import { PageHeading, Empty } from "../components/shared";

const ratingText = ["", "Chưa hài lòng", "Cần cải thiện", "Bình thường", "Hài lòng", "Rất hài lòng"];

function ReviewCard({ item, onSaved }) {
  const [rating, setRating] = useState(0);
  const [comment, setComment] = useState("");
  const [attended, setAttended] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const lock = useRef(false);
  const submitted = item.rating !== null;
  const submit = async (event) => {
    event.preventDefault();
    if (lock.current) return;
    lock.current = true;
    setBusy(true);
    setError("");
    try {
      await api(`/trial-reviews/${item.trialId}`, { method: "POST", body: { rating, comment, attended } });
      onSaved({ ...item, rating, comment: comment.trim(), reviewedAt: new Date().toISOString() });
    } catch (e) {
      setError(e.message);
    } finally {
      lock.current = false;
      setBusy(false);
    }
  };
  return <article className="panel review-card">
    <div className="review-card-heading"><h2>{item.tutorName}</h2>
      <span className={submitted ? "review-badge submitted" : "review-badge"}>{submitted ? "Đã đánh giá" : "Chờ đánh giá"}</span></div>
    <p><strong>{item.studentName}</strong> · {item.mode === "ONLINE" ? "Trực tuyến" : "Trực tiếp"}</p>
    <p className="muted">{new Date(item.lessonAt).toLocaleString("vi-VN", { timeZone: "Asia/Ho_Chi_Minh", dateStyle: "full", timeStyle: "short" })}</p>
    {submitted ? <div className="review-result" role="status">
      <p className="review-stars" aria-label={`${item.rating} trên 5 sao`}>{"★".repeat(item.rating)}<span>{"☆".repeat(5 - item.rating)}</span></p>
      <strong>{ratingText[item.rating]}</strong>
      {item.comment && <p className="review-comment">{item.comment}</p>}
      <p className="fineprint">Đánh giá đã được lưu.</p>
    </div> : <form onSubmit={submit}>
      <fieldset className="review-rating" disabled={busy}>
        <legend>Bạn thấy buổi học thế nào?</legend>
        <div className="review-rating-options">{[1, 2, 3, 4, 5].map((value) => <label key={value}>
          <input type="radio" name={`rating-${item.trialId}`} value={value} checked={rating === value}
            onChange={() => setRating(value)} required aria-label={`${value} sao`} />
          <span className={rating >= value ? "selected" : ""} aria-hidden="true">★</span>
        </label>)}</div>
        <p className="fineprint" aria-live="polite">{rating ? ratingText[rating] : "Chọn từ 1 đến 5 sao"}</p>
      </fieldset>
      <label className="field"><span>Nhận xét về buổi học (không bắt buộc)</span>
        <textarea rows={4} maxLength={2000} value={comment} disabled={busy} onChange={(e) => setComment(e.target.value)}
          placeholder="Cách giảng dạy, mức độ phù hợp và điều bạn mong gia sư cải thiện…" />
        <span className="fineprint">{comment.length}/2.000 ký tự</span>
      </label>
      <label className="review-attended"><input type="checkbox" required checked={attended} disabled={busy}
        onChange={(e) => setAttended(e.target.checked)} />Tôi xác nhận học sinh đã tham gia buổi học thử này.</label>
      {error && <p className="error" role="alert">{error}</p>}
      <button disabled={busy || !rating || !attended}>{busy ? "Đang gửi…" : "Gửi đánh giá"}</button>
    </form>}
  </article>;
}

export function ReviewsPage() {
  const [items, setItems] = useState(null);
  const [error, setError] = useState("");
  const [refresh, setRefresh] = useState(0);
  const [filter, setFilter] = useState("pending");
  useEffect(() => {
    const controller = new AbortController();
    api("/trial-reviews", { signal: controller.signal }).then((data) => { setItems(data); setError(""); })
      .catch((e) => { if (e.name !== "AbortError") setError(e.message); });
    return () => controller.abort();
  }, [refresh]);
  const pending = items?.filter((item) => item.rating === null).length || 0;
  const visible = items?.filter((item) => filter === "all" || (filter === "pending" ? item.rating === null : item.rating !== null));
  return <>
    <PageHeading eyebrow="SAU BUỔI HỌC ĐẦU TIÊN" title="Đánh giá buổi học thử"
      description="Chia sẻ trải nghiệm của con sau buổi học. Mỗi buổi học được đánh giá một lần." />
    <div className="toolbar review-toolbar">
      <label className="field"><span>Hiển thị</span><select value={filter} onChange={(e) => setFilter(e.target.value)}>
        <option value="pending">Chờ đánh giá ({pending})</option><option value="reviewed">Đã đánh giá</option><option value="all">Tất cả buổi học</option>
      </select></label>
      <button className="secondary" onClick={() => setRefresh((n) => n + 1)}>Cập nhật danh sách</button>
    </div>
    {error && <div className="error" role="alert">{error}</div>}
    {!items && !error && <p role="status">Đang tải buổi học thử…</p>}
    {items && !visible.length && <Empty title={filter === "pending" ? "Không có buổi học chờ đánh giá" : "Chưa có đánh giá"}
      text="Buổi học đã chốt lịch và đã qua giờ bắt đầu sẽ xuất hiện tại đây. Hãy xác nhận con đã tham gia trước khi gửi đánh giá." />}
    <div className="review-list">{visible?.map((item) => <ReviewCard key={item.trialId} item={item}
      onSaved={(next) => { setItems((current) => current.map((value) => value.trialId === next.trialId ? next : value)); setFilter("reviewed"); }} />)}</div>
  </>;
}
