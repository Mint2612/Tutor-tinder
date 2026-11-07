import { useEffect, useRef } from "react";

export function Field({ label, children }) {
  return (
    <label className="field">
      <span>{label}</span>
      {children}
    </label>
  );
}

export function Notice({ children }) {
  return children ? (
    <div className="notice" role="status">
      {children}
    </div>
  ) : null;
}

export function Choices({ options, value, onChange, labels = {} }) {
  return (
    <div className="choices">
      {options.map((option) => (
        <label key={option}>
          <input
            type="checkbox"
            checked={value.includes(option)}
            onChange={(e) =>
              onChange(
                e.target.checked
                  ? [...value, option]
                  : value.filter((x) => x !== option),
              )
            }
          />
          {labels[option] || option}
        </label>
      ))}
    </div>
  );
}

export function Schedule({ value, onChange }) {
  return (
    <fieldset>
      <legend>Lịch có thể học / dạy mỗi tuần</legend>
      <p className="muted">
        Chọn các buổi có thể sắp xếp. Giờ cụ thể sẽ thống nhất khi học thử.
      </p>
      <div className="schedule">
        <span></span>
        <span>Sáng</span>
        <span>Chiều</span>
        <span>Tối</span>
        {Array.from({ length: 7 }, (_, i) => (
          <div className="schedule-row" key={i}>
            <span>{i === 6 ? "Chủ nhật" : `Thứ ${i + 2}`}</span>
            {["AM", "PM", "EVENING"].map((slot, j) => {
              const key = `${i + 1}_${slot}`;
              return (
                <input
                  key={key}
                  aria-label={`${i === 6 ? "Chủ nhật" : `Thứ ${i + 2}`} ${["sáng", "chiều", "tối"][j]}`}
                  type="checkbox"
                  checked={value.includes(key)}
                  onChange={(e) =>
                    onChange(
                      e.target.checked
                        ? [...value, key]
                        : value.filter((x) => x !== key),
                    )
                  }
                />
              );
            })}
          </div>
        ))}
      </div>
    </fieldset>
  );
}

export function Availability({ slots = [] }) {
  return (
    <span>
      {slots
        .map((slot) => {
          const [day, time] = slot.split("_");
          return `${day === "7" ? "CN" : `T${Number(day) + 1}`} ${{ AM: "sáng", PM: "chiều", EVENING: "tối" }[time]}`;
        })
        .join(" · ")}
    </span>
  );
}

export function Avatar({ name, photo, large = false }) {
  return photo ? (
    <img
      className={`avatar ${large ? "large" : ""}`}
      src={photo}
      alt={`Ảnh ${name}`}
      referrerPolicy="no-referrer"
    />
  ) : (
    <span className={`avatar ${large ? "large" : ""}`}>
      {name?.trim()?.charAt(0) || "T"}
    </span>
  );
}

export function PageHeading({ eyebrow, title, description }) {
  return (
    <div className="page-heading">
      <p className="eyebrow">{eyebrow}</p>
      <h1>{title}</h1>
      <p className="muted">{description}</p>
    </div>
  );
}

export function Empty({ title, text }) {
  return (
    <div className="empty">
      <span>✦</span>
      <h2>{title}</h2>
      <p>{text}</p>
    </div>
  );
}

export function StudentPicker({
  students,
  selected,
  onChange,
  disabled = false,
}) {
  return (
    <Field label="Hồ sơ học sinh">
      <select
        disabled={disabled}
        value={selected}
        onChange={(e) => onChange(e.target.value)}
      >
        {!students.length && <option value="">Chưa có hồ sơ</option>}
        {students.map((s) => (
          <option key={s.id} value={s.id}>
            {s.nickname} · Lớp {s.grade}
          </option>
        ))}
      </select>
    </Field>
  );
}

export function Modal({ children, close, label = "Thông tin chi tiết" }) {
  const ref = useRef(null);
  useEffect(() => {
    const previous = document.activeElement;
    const dialog = ref.current;
    dialog.showModal();
    return () => {
      dialog.close();
      previous?.focus();
    };
  }, []);
  return (
    <dialog
      ref={ref}
      className="modal"
      aria-label={label}
      onCancel={(e) => {
        e.preventDefault();
        close();
      }}
      onClick={(e) => {
        if (e.target === ref.current) close();
      }}
    >
      <div className="modal-content">
        <button
          aria-label="Đóng cửa sổ"
          className="close secondary"
          onClick={close}
        >
          ×
        </button>
        {children}
      </div>
    </dialog>
  );
}
