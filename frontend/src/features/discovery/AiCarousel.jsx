import { useLayoutEffect, useRef, useState } from "react";
import { MatchingCard } from "./MatchingCard";

// Số hồ sơ gợi ý (khớp SUGGESTION_LIMIT ở backend). Danh sách cuộn vòng: hết hồ sơ cuối lại quay về hồ sơ đầu.
export const AI_SUGGESTION_COUNT = 10;
// Cuộn vô tận: danh sách được lặp COPIES lần và luôn được đưa về vùng giữa ngay khi cuộn lệch ra,
// bằng đúng bội số của một vòng (nội dung giống hệt nên mắt không thấy bước nhảy).
const COPIES = 9;
const MARGIN = 2.5; // đơn vị: số vòng tính từ hai đầu
const JUMP = 3;

export function AiCarousel({ cards, busy, onChoose }) {
  const items = cards.slice(0, AI_SUGGESTION_COUNT);
  const count = items.length;
  const loop = count > 1;
  const slides = loop ? Array.from({ length: COPIES }, () => items).flat() : items;
  const viewport = useRef(null);
  const drag = useRef(null);
  const settle = useRef(null);
  const activeRef = useRef(0);
  const stepRef = useRef(0);
  const animating = useRef(false);
  const animTimer = useRef(null);
  const inertia = useRef(0);
  const middle = Math.floor(COPIES / 2) * count;
  const positionRef = useRef(loop ? middle : 0);
  const [active, setActive] = useState(0);
  const [windowStart, setWindowStart] = useState(loop ? middle : 0);
  const [visible, setVisible] = useState(3);
  const metrics = () => {
    const element = viewport.current;
    if (!element?.children.length) return null;
    const step = stepRef.current;
    if (!step) return null;
    return { element, step, cycle: step * count };
  };
  useLayoutEffect(() => {
    const element = viewport.current;
    if (!element) return;
    let width = -1;
    const resize = () => {
      if (width === element.clientWidth) return;
      width = element.clientWidth;
      const first = element.children[0];
      const step = element.children.length > 1 ? element.children[1].getBoundingClientRect().left - first.getBoundingClientRect().left : first.getBoundingClientRect().width;
      stepRef.current = step;
      setVisible(Number(getComputedStyle(element).getPropertyValue("--carousel-columns")) || 1);
      element.style.scrollSnapType = "none";
      element.scrollTo({ left: positionRef.current * step, behavior: "instant" });
    };
    resize();
    const observer = new ResizeObserver(resize);
    observer.observe(element);
    const freeScroll = () => { clearTimeout(settle.current); element.style.scrollSnapType = "none"; };
    element.addEventListener("wheel", freeScroll, { passive: true });
    element.addEventListener("touchstart", freeScroll, { passive: true });
    return () => {
      observer.disconnect(); clearTimeout(settle.current); clearTimeout(animTimer.current); cancelAnimationFrame(inertia.current);
      element.removeEventListener("wheel", freeScroll);
      element.removeEventListener("touchstart", freeScroll);
    };
  }, [count, loop]);
  // Đưa vị trí về vùng giữa khi gần chạm hai đầu. Trả về độ dịch (px) để chỉnh lại điểm bắt đầu kéo chuột.
  const recenter = () => {
    const data = metrics();
    if (!data || !loop) return 0;
    const { element, cycle } = data;
    const left = element.scrollLeft;
    const shift = left < cycle * MARGIN ? cycle * JUMP : left > cycle * (COPIES - MARGIN) ? -cycle * JUMP : 0;
    if (shift) {
      element.style.scrollSnapType = "none";
      element.scrollLeft = left + shift;
    }
    return shift;
  };
  // Trong lúc cuộn mượt do nút/phím/chấm điều hướng, không nhảy vị trí (sẽ làm hỏng đích đến).
  const markAnimating = (ms = 800) => {
    animating.current = true;
    clearTimeout(animTimer.current);
    animTimer.current = setTimeout(() => { animating.current = false; }, ms);
  };
  const go = (direction, index) => {
    const data = metrics();
    if (!data || busy || !loop) return;
    cancelAnimationFrame(inertia.current);
    if (!drag.current) recenter();
    markAnimating();
    const { element, step } = data;
    const target = index === undefined ? element.scrollLeft / step + direction
      : Math.round((element.scrollLeft / step - index) / count) * count + index;
    element.scrollTo({ left: target * step, behavior: matchMedia("(prefers-reduced-motion: reduce)").matches ? "instant" : "smooth" });
  };
  const scrolled = () => {
    const data = metrics();
    if (!data) return;
    if (!animating.current) {
      const shift = recenter();
      if (shift && drag.current) drag.current.left += shift;
    }
    const { element, step } = data;
    positionRef.current = element.scrollLeft / step;
    const nearest = Math.round(element.scrollLeft / step);
    const index = ((nearest % count) + count) % count;
    activeRef.current = index;
    setActive(index);
    setWindowStart(Math.floor((element.scrollLeft + 1) / step));
    clearTimeout(settle.current);
    settle.current = setTimeout(() => {
      if (!loop || drag.current) return;
      const left = element.scrollLeft;
      const distance = Math.abs(left - Math.round(left / step) * step);
      element.style.scrollSnapType = distance < 20 ? "x proximity" : "none";
    }, 500);
  };
  const endDrag = (event) => {
    const current = drag.current;
    drag.current = null;
    if (!current) return;
    const element = viewport.current;
    element.classList.remove("is-dragging");
    if (element.hasPointerCapture(event.pointerId)) element.releasePointerCapture(event.pointerId);
    if (current.moved && !matchMedia("(prefers-reduced-motion: reduce)").matches) {
      // Quán tính tự tính: cộng dần vào vị trí hiện tại (không đặt đích tuyệt đối) nên vẫn nhảy về giữa được khi đang trôi.
      let speed = Math.max(-2.2, Math.min(2.2, current.velocity)); // px/ms, dương = cuộn sang phải
      let last = performance.now();
      const tick = (now) => {
        const dt = Math.min(48, now - last);
        last = now;
        element.scrollLeft += speed * dt;
        speed *= Math.pow(0.9, dt / 16);
        if (Math.abs(speed) > 0.03) inertia.current = requestAnimationFrame(tick);
      };
      cancelAnimationFrame(inertia.current);
      if (Math.abs(speed) > 0.1) inertia.current = requestAnimationFrame(tick);
    }
  };
  return <section className="ai-carousel" aria-label="Gợi ý bằng AI" aria-roledescription="carousel">
    <div className="ai-carousel-heading">
      <p>{count} hồ sơ được gợi ý</p>
      <div className="ai-carousel-controls">
        <button className="secondary" aria-label="Hồ sơ trước" disabled={busy || !loop} onClick={() => go(-1)}>←</button>
        <button className="secondary" aria-label="Hồ sơ tiếp theo" disabled={busy || !loop} onClick={() => go(1)}>→</button>
      </div>
    </div>
    <div ref={viewport} className="ai-carousel-viewport" tabIndex={0} onScroll={scrolled}
      aria-label="Danh sách cuộn ngang. Vuốt hoặc dùng phím mũi tên để xem hồ sơ."
      onKeyDown={(event) => {
        if (event.target !== event.currentTarget) return;
        if (event.key === "ArrowLeft" || event.key === "ArrowRight") { event.preventDefault(); go(event.key === "ArrowLeft" ? -1 : 1); }
      }}
      onPointerDown={(event) => {
        cancelAnimationFrame(inertia.current);
        // Touch and trackpad scrolling remain native; mouse users can also grab the list.
        if (!event.target.closest("button, summary, a, input, textarea, select")) event.currentTarget.style.scrollSnapType = "none";
        if (event.pointerType !== "mouse" || event.button !== 0 || busy || !loop || event.target.closest("button, summary, a, input, textarea, select")) return;
        const element = event.currentTarget;
        element.scrollTo({ left: element.scrollLeft, behavior: "instant" });
        drag.current = { x: event.clientX, left: element.scrollLeft, lastX: event.clientX, time: event.timeStamp, velocity: 0, moved: false };
      }}
      onPointerMove={(event) => {
        const current = drag.current;
        if (!current || event.pointerType !== "mouse") return;
        const distance = event.clientX - current.x;
        if (!current.moved && Math.abs(distance) < 5) return;
        current.moved = true;
        const elapsed = event.timeStamp - current.time;
        if (elapsed > 0) current.velocity = current.velocity * 0.4 + (current.lastX - event.clientX) / elapsed * 0.6;
        current.lastX = event.clientX;
        current.time = event.timeStamp;
        const element = event.currentTarget;
        if (!element.hasPointerCapture(event.pointerId)) element.setPointerCapture(event.pointerId);
        element.style.scrollSnapType = "none";
        element.classList.add("is-dragging");
        element.scrollLeft = current.left - distance;
      }}
      onPointerUp={endDrag} onPointerCancel={endDrag} onLostPointerCapture={endDrag}>
      {slides.map((card, index) => <div className="ai-carousel-slide" key={`${card.studentId}:${card.tutorId}:${index}`}
        role="group" aria-roledescription="slide" aria-label={`Hồ sơ ${index % count + 1}/${count}`}
        aria-hidden={index < windowStart || index > windowStart + visible} inert={index < windowStart || index > windowStart + visible}>
        <MatchingCard card={card} swipe={false} compact busy={busy} onChoose={(like) => onChoose(card, like)} />
      </div>)}
    </div>
    {loop && <div className="ai-carousel-dots" aria-label="Chọn hồ sơ">{items.map((card, index) => (
      <button key={`${card.studentId}:${card.tutorId}`} aria-label={`Xem hồ sơ ${index + 1}`} aria-current={active === index ? "true" : undefined}
        disabled={busy} onClick={() => go(0, index)} />
    ))}</div>}
    <p className="fineprint">Kéo hoặc vuốt ngang để xem thêm hồ sơ.</p>
  </section>;
}
