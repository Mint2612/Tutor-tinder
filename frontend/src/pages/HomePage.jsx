import { useWorkspace } from "../hooks/useWorkspace";
export function HomePage() {
  const { user, navigate, setPage } = useWorkspace();
  return (
    <section className="landing">
      <div className="hero">
        <p className="eyebrow">MỖI HỌC SINH, MỘT CÁCH TỎA SÁNG</p>
        <h1>
          Đúng gia sư.
          <br />
          <em>Đúng cách học.</em>
          <br />
          Cùng con tiến xa.
        </h1>
        <p className="intro">
          Không chỉ là một người dạy giỏi. Tìm người hiểu cách con học, kiên
          nhẫn cùng con và phù hợp với gia đình bạn.
        </p>
        <div className="hero-actions">
          <button
            onClick={() =>
              user
                ? user.role === "PARENT"
                  ? navigate("students")
                  : navigate(user.role === "TUTOR" ? "tutor" : "admin")
                : setPage("register")
            }
          >
            {user ? "Vào không gian của bạn" : "Tìm gia sư cho con"}{" "}
            <span>↗</span>
          </button>
          {!user && (
            <button className="text-button" onClick={() => setPage("register")}>
              Tôi muốn trở thành gia sư
            </button>
          )}
        </div>
        <div className="trust-line">
          <span>✓ Hồ sơ được duyệt</span>
          <span>✓ Gợi ý có giải thích</span>
          <span>✓ Chủ động học thử</span>
        </div>
      </div>
      <div className="hero-art" aria-hidden="true">
        <div className="orbit orbit-one"></div>
        <div className="orbit orbit-two"></div>
        <span className="art-note note-one">Một cách học phù hợp ✦</span>
        <div className="illustration">
          <div className="book">
            <span>abc</span>
            <span>√x</span>
          </div>
          <div className="spark">✦</div>
          <div className="art-caption">
            Kết nối hôm nay.
            <br />
            <strong>Tiến bộ mỗi ngày.</strong>
          </div>
        </div>
        <span className="art-note note-two">Hiểu con, đồng hành cùng con</span>
      </div>
      <div className="steps">
        {[
          [
            "01",
            "Chia sẻ về con",
            "Hồ sơ và khảo sát ngắn giúp làm rõ nhu cầu học tập.",
          ],
          [
            "02",
            "Khám phá người phù hợp",
            "Lướt hồ sơ đã xác minh, đọc lý do gợi ý và tìm hiểu.",
          ],
          [
            "03",
            "Bắt đầu bằng học thử",
            "Gửi lời mời. Hai bên kết nối khi gia sư chấp nhận.",
          ],
        ].map(([n, title, text]) => (
          <article key={n}>
            <span className="step-number">{n}</span>
            <h3>{title}</h3>
            <p>{text}</p>
          </article>
        ))}
      </div>
    </section>
  );
}
