// Verification levels shown to a tutor (levels 1-3; level 4, the post-trial rating, comes later).
function identityText(tutor) {
  switch (tutor?.workflow_state) {
    case "APPROVED":
      return "Quản trị viên đã đối chiếu và xác minh danh tính của bạn.";
    case "PENDING_REVIEW":
      return "Hồ sơ đang chờ quản trị viên xem xét.";
    case "REJECTED":
      return `Hồ sơ bị từ chối${tutor.rejection_reason ? `: ${tutor.rejection_reason}` : ""}. Hãy chỉnh sửa và gửi lại.`;
    default:
      return "Tải minh chứng danh tính rồi gửi hồ sơ để được xác minh. Bắt buộc để hiển thị với phụ huynh.";
  }
}

export function VerificationBadges({ tutor }) {
  const levels = [
    {
      level: 1,
      name: "Email đã xác minh",
      earned: true,
      text: "Bạn đã xác minh email khi kích hoạt tài khoản.",
    },
    {
      level: 2,
      name: "Đã xác minh danh tính",
      earned: tutor?.workflow_state === "APPROVED",
      text: identityText(tutor),
    },
    {
      level: 3,
      name: "Tự khai (chưa xác minh)",
      earned: !!tutor?.achievements,
      text: tutor?.achievements
        ? "Phụ huynh thấy học lực, giải thưởng bạn ghi với nhãn “Tự khai”."
        : "Tùy chọn: ghi học lực, giải thưởng trong hồ sơ giảng dạy bên dưới.",
    },
  ];
  return (
    <section className="panel">
      <h2>Huy hiệu xác minh</h2>
      <p className="muted">
        Phụ huynh thấy các huy hiệu này trên hồ sơ của bạn. Cần đạt cấp 2 để
        xuất hiện trong gợi ý.
      </p>
      <ul className="badges">
        {levels.map((l) => (
          <li key={l.level} className={l.earned ? "badge-item earned" : "badge-item"}>
            <span className="badge-level" aria-hidden="true">
              {l.level}
            </span>
            <div>
              <strong>{l.name}</strong>{" "}
              <span className={l.earned ? "status approved" : "status"}>
                {l.earned ? "Đã có" : "Chưa có"}
              </span>
              <p className="muted">{l.text}</p>
            </div>
          </li>
        ))}
      </ul>
    </section>
  );
}
