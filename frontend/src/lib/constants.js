export const money = (value) =>
  new Intl.NumberFormat("vi-VN").format(value) + "đ";
export const statusText = {
  DRAFT: "Bản nháp",
  PENDING_REVIEW: "Đang chờ duyệt",
  APPROVED: "Đã xác minh",
  REJECTED: "Đã từ chối",
  PENDING: "Đang chờ phản hồi",
  ACCEPTED: "Đã kết nối",
  EXPIRED: "Đã hết hạn",
};
export const levelText = {
  STRONG: "Rất đáng cân nhắc",
  GOOD: "Phù hợp để tìm hiểu",
  POSSIBLE: "Có thể phù hợp",
};
export const subjects = [
  "Toán",
  "Ngữ văn",
  "Tiếng Anh",
  "Vật lý",
  "Hóa học",
  "Sinh học",
  "Tin học",
];
export const styles = [
  "Kiên nhẫn, hướng dẫn từng bước",
  "Trực quan, nhiều ví dụ",
  "Thực hành, tương tác",
];
export const questions = [
  [
    "goal",
    "Mục tiêu học tập chính?",
    [
      ["foundation", "Củng cố nền tảng"],
      ["exam", "Chuẩn bị thi"],
      ["confidence", "Tự tin hơn"],
    ],
  ],
  [
    "learning",
    "Con tiếp thu tốt nhất khi nào?",
    [
      ["visual", "Có hình ảnh minh họa"],
      ["practice", "Tự thực hành"],
      ["discussion", "Được trao đổi"],
    ],
  ],
  [
    "difficulty",
    "Con đang cần hỗ trợ điều gì?",
    [
      ["understanding", "Hiểu bản chất"],
      ["attention", "Duy trì tập trung"],
      ["motivation", "Động lực học"],
    ],
  ],
  [
    "pace",
    "Nhịp học phù hợp với con?",
    [
      ["step_by_step", "Từng bước chắc chắn"],
      ["balanced", "Cân bằng"],
      ["challenge", "Nhiều thử thách"],
    ],
  ],
  [
    "support",
    "Mức độ hướng dẫn con mong muốn?",
    [
      ["support", "Hướng dẫn sát sao"],
      ["independent", "Khuyến khích tự lập"],
      ["feedback", "Phản hồi thường xuyên"],
    ],
  ],
  [
    "lesson",
    "Hoạt động học con yêu thích?",
    [
      ["examples", "Ví dụ thực tế"],
      ["practice", "Bài tập thực hành"],
      ["review", "Ôn tập và hệ thống"],
    ],
  ],
];
export const blankStudent = {
  nickname: "",
  grade: 6,
  subjects: ["Toán"],
  province: "",
  mode: "ONLINE",
  budget: 200000,
  availability: [],
  needs: "",
  survey: {},
  consent: false,
};
export const blankTutor = {
  displayName: "",
  photoUrl: "",
  subjects: ["Toán"],
  grades: [6],
  modes: ["ONLINE"],
  province: "",
  hourlyRate: 150000,
  methodology: "",
  style: styles[0],
  background: "",
  achievements: "",
  experience: 0,
  availability: [],
};
