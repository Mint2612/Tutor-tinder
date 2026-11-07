import { DAY_NAMES, slotProblems } from "../lib/schedule";

// Free time as a list of exact ranges instead of a morning/afternoon/evening grid. `value` is a
// list of { day, start, end } rows; invalid rows stay visible with a message until fixed.
export function TimeSlots({ value, onChange }) {
  const problems = slotProblems(value);
  const update = (index, patch) =>
    onChange(value.map((row, i) => (i === index ? { ...row, ...patch } : row)));
  const add = () => {
    const last = value.at(-1);
    onChange([
      ...value,
      last
        ? {
            day: (last.day % 7) + 1,
            start: last.start || "18:00",
            end: last.end || "20:00",
          }
        : { day: 1, start: "18:00", end: "20:00" },
    ]);
  };
  return (
    <fieldset>
      <legend>Thời gian rảnh có thể dạy mỗi tuần</legend>
      <p className="muted">
        Thêm từng khung giờ bạn có thể dạy. Phụ huynh chọn buổi sáng, chiều hoặc
        tối nên khung giờ của bạn cần trùng ít nhất 1 giờ với buổi đó.
      </p>
      {value.length === 0 && (
        <p className="muted">Chưa có khung giờ nào. Hãy thêm ít nhất một.</p>
      )}
      <ul className="time-slots">
        {value.map((row, i) => (
          <li key={i} className="time-slot">
            <select
              aria-label={`Ngày dạy ${i + 1}`}
              value={row.day}
              onChange={(e) => update(i, { day: Number(e.target.value) })}
            >
              {DAY_NAMES.map((name, d) => (
                <option key={name} value={d + 1}>
                  {name}
                </option>
              ))}
            </select>
            <input
              type="time"
              aria-label={`Giờ bắt đầu ${i + 1}`}
              value={row.start}
              onChange={(e) => update(i, { start: e.target.value })}
            />
            <span aria-hidden="true">–</span>
            <input
              type="time"
              aria-label={`Giờ kết thúc ${i + 1}`}
              value={row.end}
              onChange={(e) => update(i, { end: e.target.value })}
            />
            <button
              type="button"
              className="secondary small"
              aria-label={`Xóa khung giờ ${i + 1}`}
              onClick={() => onChange(value.filter((_, j) => j !== i))}
            >
              Xóa
            </button>
            {problems[i] && <p className="field-error">{problems[i]}</p>}
          </li>
        ))}
      </ul>
      <button type="button" className="secondary" onClick={add}>
        + Thêm thời gian rảnh
      </button>
    </fieldset>
  );
}
