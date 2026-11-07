import { useEffect, useState } from "react";
import { useWorkspace } from "../../hooks/useWorkspace";
import { api } from "../../services/api";

export function MatchNotifications() {
  const { user, run, navigate, busy, setSelected } = useWorkspace();
  const [items, setItems] = useState([]);
  useEffect(() => {
    const controller = new AbortController();
    const load = () => api("/matching/notifications", { signal: controller.signal })
      .then(setItems).catch(() => {});
    load();
    const timer = setInterval(load, 10000);
    return () => { controller.abort(); clearInterval(timer); };
  }, [user.id]);
  if (!items.length) return null;
  return <aside className="match-notifications" aria-label="Thông báo Match">
    <p role="status">♡ {items.length} Match mới. Hai bên đã cùng OK!</p>
    <button disabled={busy} onClick={() => {
      if (user.role === "PARENT") setSelected(items[0].studentId);
      navigate("interests");
    }}>Xem Match</button>
    <button className="secondary" disabled={busy} onClick={() => run(async () => {
      await Promise.all(items.map((item) => api(`/matching/notifications/${item.id}/read`, { method: "POST" })));
      setItems([]);
    })}>Đánh dấu đã đọc</button>
  </aside>;
}
