import { useState } from "react";
import { Field, Choices, Schedule } from "../../components/shared";
import { subjects, styles, blankTutor } from "../../lib/constants";

export function TutorForm({ initial, save, busy }) {
  const [form, setForm] = useState(
    initial
      ? {
          displayName: initial.display_name,
          photoUrl: initial.photo_url,
          subjects: initial.subjects,
          grades: initial.grades,
          modes: initial.modes,
          province: initial.province,
          hourlyRate: initial.hourly_rate,
          methodology: initial.teaching_description,
          style: initial.teaching_style,
          background: initial.background || "",
          experience: initial.experience_years,
          availability: initial.availability,
        }
      : blankTutor,
  );
  const [validationError, setValidationError] = useState("");
  const set = (key, value) => {
    setValidationError("");
    setForm((f) => ({ ...f, [key]: value }));
  };
  return (
    <form
      className="panel form-panel"
      onSubmit={(e) => {
        e.preventDefault();
        if (
          [form.subjects, form.grades, form.modes, form.availability].some(
            (values) => !values.length,
          )
        ) {
          setValidationError(
            "Vui lòng chọn môn, lớp, hình thức và ít nhất một buổi có thể dạy.",
          );
          return;
        }
        save(form);
      }}
    >
      <h2>Hồ sơ giảng dạy</h2>
      {validationError && (
        <div className="error" role="alert">
          {validationError}
        </div>
      )}
      <p className="muted">
        Mọi thay đổi sẽ đưa hồ sơ về bản nháp và cần gửi duyệt lại.
      </p>
      <div className="form-grid">
        {[
          ["displayName", "Tên hiển thị", "text", 120],
          ["photoUrl", "Đường dẫn ảnh công khai HTTPS (tùy chọn)", "url", 500],
          ["province", "Tỉnh / thành phố", "text", 100],
          ["hourlyRate", "Học phí mỗi giờ (đ)", "number"],
          ["experience", "Số năm kinh nghiệm", "number"],
        ].map(([key, label, type, max]) => (
          <Field key={key} label={label}>
            <input
              type={type}
              required={key !== "photoUrl"}
              min={key === "hourlyRate" ? 1 : 0}
              max={key === "experience" ? 60 : 10000000}
              maxLength={max}
              value={form[key]}
              onChange={(e) =>
                set(key, type === "number" ? +e.target.value : e.target.value)
              }
            />
          </Field>
        ))}
        <Field label="Phong cách giảng dạy">
          <select
            value={form.style}
            onChange={(e) => set("style", e.target.value)}
          >
            {styles.map((s) => (
              <option key={s}>{s}</option>
            ))}
          </select>
        </Field>
      </div>
      <fieldset>
        <legend>Môn giảng dạy</legend>
        <Choices
          options={subjects}
          value={form.subjects}
          onChange={(v) => set("subjects", v)}
        />
      </fieldset>
      <fieldset>
        <legend>Lớp giảng dạy</legend>
        <Choices
          options={Array.from({ length: 12 }, (_, i) => i + 1)}
          value={form.grades}
          onChange={(v) => set("grades", v)}
        />
      </fieldset>
      <fieldset>
        <legend>Hình thức giảng dạy</legend>
        <Choices
          options={["ONLINE", "IN_PERSON"]}
          labels={{ ONLINE: "Trực tuyến", IN_PERSON: "Trực tiếp" }}
          value={form.modes}
          onChange={(v) => set("modes", v)}
        />
      </fieldset>
      <Field label="Học vấn, bằng cấp và kinh nghiệm">
        <textarea
          required
          maxLength={3000}
          rows={3}
          value={form.background}
          onChange={(e) => set("background", e.target.value)}
        />
      </Field>
      <Field label="Phương pháp giảng dạy">
        <textarea
          required
          maxLength={3000}
          rows={3}
          value={form.methodology}
          onChange={(e) => set("methodology", e.target.value)}
        />
      </Field>
      <Schedule
        value={form.availability}
        onChange={(v) => set("availability", v)}
      />
      <button disabled={busy}>Lưu bản nháp</button>
    </form>
  );
}
