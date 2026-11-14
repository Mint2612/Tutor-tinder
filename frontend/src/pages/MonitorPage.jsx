import { useEffect, useState } from "react";
import { api } from "../services/api";
import { PageHeading } from "../components/shared";
import { useWorkspace } from "../hooks/useWorkspace";

const REFRESH_MS = 30000;
const number = (value) => new Intl.NumberFormat("vi-VN").format(value ?? 0);
const dateTime = (iso) => (iso ? new Date(iso).toLocaleString("vi-VN") : "—");
const shortDay = (iso) => {
  const [, month, day] = iso.split("-");
  return `${day}/${month}`;
};

function since(iso) {
  if (!iso) return "";
  const minutes = Math.max(0, Math.round((Date.now() - new Date(iso)) / 60000));
  if (minutes < 60) return `${minutes} phút`;
  const hours = Math.round(minutes / 60);
  return hours < 48 ? `${hours} giờ` : `${Math.round(hours / 24)} ngày`;
}

function uptime(seconds) {
  const days = Math.floor(seconds / 86400);
  const hours = Math.floor((seconds % 86400) / 3600);
  const minutes = Math.floor((seconds % 3600) / 60);
  return days ? `${days} ngày ${hours} giờ` : hours ? `${hours} giờ ${minutes} phút` : `${minutes} phút`;
}

function Stat({ label, value, hint }) {
  return (
    <div className="monitor-stat">
      <span className="monitor-stat-value">{number(value)}</span>
      <span className="monitor-stat-label">{label}</span>
      {hint && <span className="monitor-stat-hint">{hint}</span>}
    </div>
  );
}

// One series per chart, one hue, bars anchored at zero; the number sits at the end of each bar.
function Funnel({ rows }) {
  const max = Math.max(1, ...rows.map((row) => row.value));
  return (
    <ol className="monitor-funnel">
      {rows.map((row) => (
        <li key={row.label}>
          <span className="monitor-funnel-label">{row.label}</span>
          <span className="monitor-funnel-track" aria-hidden="true">
            <span style={{ width: `${(row.value / max) * 100}%` }} />
          </span>
          <strong>{number(row.value)}</strong>
        </li>
      ))}
    </ol>
  );
}

function DayBars({ title, days, field }) {
  const max = Math.max(1, ...days.map((d) => d[field]));
  const total = days.reduce((sum, d) => sum + d[field], 0);
  return (
    <figure className="monitor-days">
      <figcaption>
        <strong>{title}</strong>
        <span>{number(total)} trong 7 ngày</span>
      </figcaption>
      <div className="monitor-bars" role="img" aria-label={`${title}: ${days.map((d) => `${shortDay(d.day)} ${d[field]}`).join(", ")}`}>
        {days.map((d) => (
          <div key={d.day} className="monitor-bar" title={`${shortDay(d.day)}: ${d[field]}`}>
            <span className="monitor-bar-value">{d[field] || ""}</span>
            <span className="monitor-bar-fill" style={{ height: `${(d[field] / max) * 100}%` }} />
            <span className="monitor-bar-day">{shortDay(d.day)}</span>
          </div>
        ))}
      </div>
    </figure>
  );
}

export function MonitorPage() {
  const { navigate } = useWorkspace();
  const [data, setData] = useState(null);
  const [error, setError] = useState("");
  const [loadedTick, setLoadedTick] = useState(-1);
  const [auto, setAuto] = useState(true);
  const [tick, setTick] = useState(0);
  const loading = loadedTick !== tick;
  useEffect(() => {
    const controller = new AbortController();
    api("/admin/monitor", { signal: controller.signal })
      .then((snapshot) => {
        setData(snapshot);
        setError("");
      })
      .catch((e) => {
        if (e.name !== "AbortError") setError(e.message);
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoadedTick(tick);
      });
    return () => controller.abort();
  }, [tick]);
  useEffect(() => {
    if (!auto) return;
    const timer = setInterval(() => setTick((n) => n + 1), REFRESH_MS);
    return () => clearInterval(timer);
  }, [auto]);

  const m = data?.matching;
  const attention = data && [
    { key: "pending", count: data.tutors.pendingReview, text: "hồ sơ gia sư chờ duyệt", action: ["admin", "Mở hàng chờ"],
      note: data.tutors.oldestPendingSince && `Lâu nhất: ${since(data.tutors.oldestPendingSince)}` },
    { key: "confirm", count: m.awaitingConfirmation, text: "Match đang chờ hai bên xác nhận kết nối" },
    { key: "expired", count: m.confirmationExpired, text: "Match quá hạn xác nhận (sẽ đóng khi được mở)" },
    { key: "trial", count: data.trials.pending, text: "đề xuất học thử chờ phản hồi" },
  ].filter((item) => item.count > 0);

  return (
    <>
      <PageHeading
        eyebrow="QUẢN TRỊ & TIN CẬY"
        title="Giám sát hệ thống"
        description="Số liệu tổng hợp về tài khoản, xác minh gia sư, luồng Match và học thử. Không hiển thị danh tính hay thông tin liên hệ."
      />
      <div className="toolbar monitor-toolbar">
        <button className="secondary" disabled={loading} onClick={() => setTick((n) => n + 1)}>
          {loading ? "Đang cập nhật…" : "Cập nhật ngay"}
        </button>
        <label className="monitor-auto">
          <input type="checkbox" checked={auto} onChange={(e) => setAuto(e.target.checked)} />
          Tự cập nhật mỗi 30 giây
        </label>
        {data && <span className="muted">Cập nhật lúc {dateTime(data.generatedAt)}</span>}
      </div>
      {error && (
        <div className="error" role="alert">
          {error} <button onClick={() => setTick((n) => n + 1)}>Thử lại</button>
        </div>
      )}
      {!data && !error && <p role="status">Đang tải số liệu…</p>}
      {data && (
        <div className="monitor">
          <section className="panel monitor-attention" aria-label="Việc cần xử lý">
            <h2>Cần xử lý</h2>
            {attention.length === 0 ? (
              <p className="muted">Không có việc tồn đọng.</p>
            ) : (
              <ul>
                {attention.map((item) => (
                  <li key={item.key}>
                    <strong>{number(item.count)}</strong> {item.text}
                    {item.note && <span className="muted"> · {item.note}</span>}
                    {item.action && (
                      <button className="secondary small" onClick={() => navigate(item.action[0])}>
                        {item.action[1]}
                      </button>
                    )}
                  </li>
                ))}
              </ul>
            )}
            {data.waitingTutors.length > 0 && (
              <ul className="monitor-waiting" aria-label="Gia sư chờ duyệt lâu nhất">
                {data.waitingTutors.map((t) => (
                  <li key={t.id}>
                    <span>{t.displayName || "Hồ sơ chưa đặt tên"}</span>
                    <span className="muted">chờ {since(t.submittedAt)}</span>
                  </li>
                ))}
              </ul>
            )}
          </section>

          <section className="panel" aria-label="Tài khoản và hồ sơ">
            <h2>Tài khoản & hồ sơ</h2>
            <div className="monitor-stats">
              <Stat label="Phụ huynh" value={data.accounts.parents} />
              <Stat label="Gia sư" value={data.accounts.tutors} />
              <Stat label="Quản trị viên" value={data.accounts.admins} />
              <Stat label="Tài khoản mới (7 ngày)" value={data.accounts.newLast7Days} />
              <Stat label="Phiên đăng nhập còn hạn" value={data.accounts.activeSessions} />
              <Stat label="Hồ sơ học sinh" value={data.students.total} hint={`${number(data.students.hidden)} đang ẩn`} />
            </div>
            <h3>Gia sư theo trạng thái</h3>
            <div className="monitor-stats">
              <Stat label="Đã duyệt" value={data.tutors.approved} hint={`${number(data.tutors.hidden)} ẩn · ${number(data.tutors.paused)} tạm dừng`} />
              <Stat label="Chờ duyệt" value={data.tutors.pendingReview} />
              <Stat label="Bản nháp" value={data.tutors.draft} />
              <Stat label="Bị từ chối" value={data.tutors.rejected} />
            </div>
          </section>

          <section className="panel" aria-label="Luồng Match">
            <h2>Luồng Match & học thử</h2>
            <Funnel
              rows={[
                { label: "Một bên đã OK, chờ bên kia", value: m.waiting },
                { label: "Đã Match", value: m.matched },
                { label: "Đang chờ xác nhận kết nối", value: m.awaitingConfirmation },
                { label: "Đã kết nối (hai bên xác nhận)", value: m.connected },
                { label: "Học thử đang chờ phản hồi", value: data.trials.pending },
                { label: "Học thử đã chốt giờ", value: data.trials.confirmed },
              ]}
            />
            <p className="fineprint">
              Đã đóng: {number(m.skipped)} bỏ qua · {number(m.expired)} hết hạn · {number(m.confirmationExpired)} quá hạn xác nhận. Học thử:{" "}
              {number(data.trials.cancelled)} hủy · {number(data.trials.expired)} hết hạn · {number(data.trials.reviews)} đánh giá
              {data.trials.averageRating != null && ` (trung bình ${data.trials.averageRating.toFixed(1)}/5)`}. Thông báo chưa đọc:{" "}
              {number(data.unreadNotifications)}.
            </p>
          </section>

          <section className="panel" aria-label="Hoạt động 7 ngày">
            <h2>Hoạt động 7 ngày gần nhất</h2>
            <div className="monitor-days-grid">
              <DayBars title="Tài khoản mới" days={data.activity} field="newAccounts" />
              <DayBars title="Lượt OK đầu tiên" days={data.activity} field="oks" />
              <DayBars title="Match mới" days={data.activity} field="matches" />
              <DayBars title="Đề xuất học thử" days={data.activity} field="trials" />
            </div>
            <p className="fineprint">Ngày tính theo giờ Việt Nam. Mỗi biểu đồ có thang riêng.</p>
          </section>

          <section className="panel" aria-label="Máy chủ">
            <h2>Máy chủ</h2>
            <ul className="monitor-server">
              <li>
                <span>Cơ sở dữ liệu</span>
                <strong className={data.server.database ? "ok" : "bad"}>{data.server.database ? "✓ Hoạt động" : "✕ Không phản hồi"}</strong>
              </li>
              <li>
                <span>Gợi ý AI</span>
                <strong>{data.server.aiConfigured ? "Đã cấu hình khóa API" : "Chưa cấu hình — dùng luật dự phòng"}</strong>
              </li>
              <li>
                <span>Thời gian chạy</span>
                <strong>{uptime(data.server.uptimeSeconds)}</strong>
              </li>
              <li>
                <span>Khởi động lúc</span>
                <strong>{dateTime(data.server.startedAt)}</strong>
              </li>
              <li>
                <span>Java</span>
                <strong>{data.server.javaVersion}</strong>
              </li>
            </ul>
          </section>
        </div>
      )}
    </>
  );
}
