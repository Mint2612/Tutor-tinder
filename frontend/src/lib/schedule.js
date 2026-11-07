// Weekly availability. A slot is "{day}_{when}": day 1 = Monday ... 7 = Sunday, and `when` is an
// exact range "HH:MM-HH:MM" (what tutors enter) or a coarse block AM/PM/EVENING (what parents
// still pick, and what older profiles contain). The server compares both as time ranges.
export const DAY_NAMES = [
  "Thứ 2",
  "Thứ 3",
  "Thứ 4",
  "Thứ 5",
  "Thứ 6",
  "Thứ 7",
  "Chủ nhật",
];
const BLOCKS = {
  AM: ["06:00", "12:00"],
  PM: ["12:00", "18:00"],
  EVENING: ["18:00", "22:00"],
};
const BLOCK_NAMES = { AM: "sáng", PM: "chiều", EVENING: "tối" };

const shortDay = (day) => (day === 7 ? "CN" : `T${day + 1}`);

// "1_18:00-20:00" or "1_EVENING"  ->  { day: 1, start: "18:00", end: "20:00" }
export function parseSlot(token) {
  const [day, when = ""] = token.split("_");
  const block = BLOCKS[when];
  if (block) return { day: Number(day), start: block[0], end: block[1] };
  const [start = "", end = ""] = when.split("-");
  return { day: Number(day), start, end };
}

export const toToken = ({ day, start, end }) => `${day}_${start}-${end}`;

const minutes = (time) => {
  const m = /^(\d{2}):(\d{2})$/.exec(time);
  return m ? Number(m[1]) * 60 + Number(m[2]) : NaN;
};

// One message per row ("" when the row is fine); mirrors the server's rules.
export function slotProblems(rows) {
  return rows.map((row, i) => {
    const start = minutes(row.start);
    const end = minutes(row.end);
    if (Number.isNaN(start) || Number.isNaN(end))
      return "Chọn giờ bắt đầu và giờ kết thúc.";
    if (end <= start) return "Giờ kết thúc phải sau giờ bắt đầu.";
    if (end - start < 30) return "Mỗi khung giờ cần ít nhất 30 phút.";
    const clash = rows.some(
      (other, j) =>
        j !== i &&
        other.day === row.day &&
        minutes(other.start) < end &&
        start < minutes(other.end),
    );
    return clash ? "Trùng với một khung giờ khác trong cùng ngày." : "";
  });
}

// Readable text for display: "T2 18:00–20:00", or "T2 tối" for a coarse block.
export function describeSlot(token) {
  const [day, when = ""] = token.split("_");
  if (BLOCK_NAMES[when]) return `${shortDay(Number(day))} ${BLOCK_NAMES[when]}`;
  const { start, end } = parseSlot(token);
  return `${shortDay(Number(day))} ${start}–${end}`;
}

export function sortSlots(tokens) {
  const key = (token) => {
    const { day, start } = parseSlot(token);
    return day * 10000 + (minutes(start) || 0);
  };
  return [...tokens].sort((a, b) => key(a) - key(b));
}
