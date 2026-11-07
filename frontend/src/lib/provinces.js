// The 34 provincial-level units of Vietnam (6 centrally governed cities and 28 provinces) after the
// 2025 reorganisation. Names carry no "Tỉnh"/"Thành phố" prefix, so they match what profiles
// already store ("Hà Nội", "Hồ Chí Minh"). In-person matching compares these strings.
// The server keeps the same list (backend Provinces.java) and rejects anything else: keep the two
// in step.
export const PROVINCES = [
  "An Giang",
  "Bắc Ninh",
  "Cà Mau",
  "Cần Thơ",
  "Cao Bằng",
  "Đà Nẵng",
  "Đắk Lắk",
  "Điện Biên",
  "Đồng Nai",
  "Đồng Tháp",
  "Gia Lai",
  "Hà Nội",
  "Hà Tĩnh",
  "Hải Phòng",
  "Hồ Chí Minh",
  "Huế",
  "Hưng Yên",
  "Khánh Hòa",
  "Lai Châu",
  "Lâm Đồng",
  "Lạng Sơn",
  "Lào Cai",
  "Nghệ An",
  "Ninh Bình",
  "Phú Thọ",
  "Quảng Ngãi",
  "Quảng Ninh",
  "Quảng Trị",
  "Sơn La",
  "Tây Ninh",
  "Thái Nguyên",
  "Thanh Hóa",
  "Tuyên Quang",
  "Vĩnh Long",
];

// Lower case without accents, so "dong nai" finds "Đồng Nai".
export const searchKey = (text) =>
  text
    .normalize("NFD")
    .replace(/\p{M}/gu, "")
    .toLowerCase()
    .replace(/đ/g, "d")
    .trim();
