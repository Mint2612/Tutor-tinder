const steps = [
  ['01', 'Chia sẻ nhu cầu', 'Phụ huynh tạo hồ sơ học sinh và mô tả cách học phù hợp.'],
  ['02', 'Tìm người đồng hành', 'Lọc điều kiện học tập và khám phá gia sư phù hợp.'],
  ['03', 'Bắt đầu học thử', 'Gửi yêu cầu để hai bên có cơ hội tìm hiểu nhau.'],
]

export default function App() {
  return (
    <main className="container">
      <header>
        <span className="brand">TutorMatch<span>.</span></span>
        <span className="badge">Đang phát triển</span>
      </header>
      <section className="hero">
        <p className="eyebrow">Học cùng người phù hợp</p>
        <h1>Một người thầy phù hợp.<br /><span>Một khởi đầu tốt hơn.</span></h1>
        <p className="intro">Kết nối phụ huynh với gia sư dựa trên nhu cầu học tập, phương pháp dạy và những điều giúp con tiến bộ.</p>
        <p className="notice">Đây là phiên bản khởi tạo của dự án. Chức năng tài khoản, gợi ý gia sư và yêu cầu học thử đang được xây dựng.</p>
      </section>
      <section className="steps" aria-label="Cách hoạt động">
        {steps.map(([number, title, description]) => (
          <article key={number}><span className="number">{number}</span><h2>{title}</h2><p>{description}</p></article>
        ))}
      </section>
      <footer>TutorMatch · Cùng xây dựng hành trình học tập tốt hơn.</footer>
    </main>
  )
}
