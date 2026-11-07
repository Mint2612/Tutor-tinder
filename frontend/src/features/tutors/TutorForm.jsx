import { useState } from "react";
import { Field, Choices } from "../../components/shared";
import { TimeSlots } from "../../components/TimeSlots";
import { subjects, styles, blankTutor } from "../../lib/constants";
import { parseSlot, toToken, slotProblems } from "../../lib/schedule";

export function TutorForm({ initial, save, busy }) {
  const [form, setForm] = useState(
    initial
      ? {
          displayName: initial.display_name,
          // Kept only so the request stays valid; the photo is changed by uploading, not here.
          photoUrl: initial.photo_url ?? "",
          subjects: initial.subjects,
          grades: initial.grades,
          modes: initial.modes,
          province: initial.province,
          hourlyRate: initial.hourly_rate,
          methodology: initial.teaching_description,
          style: initial.teaching_style,
          background: initial.background || "",
          achievements: initial.achievements || "",
          experience: initial.experience_years,
          // Rows { day, start, end }; older coarse blocks open as their equivalent time range.
          availability: initial.availability.map(parseSlot),
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
            "Vui lòng chọn môn, lớp, hình thức và thêm ít nhất một khung giờ rảnh.",
          );
          return;
        }
        if (slotProblems(form.availability).some(Boolean)) {
          setValidationError(
            "Hãy sửa các khung giờ rảnh được đánh dấu bên dưới.",
          );
          return;
        }
        save({ ...form, availability: form.availability.map(toToken) });
      }}
    >
      <h2>Hồ sơ giảng dạy</h2>
      {validationError && (
        <div className="error" role="alert">
          {validationError}
        </div>
      )}
      <p className="muted">
        Thông tin và ảnh sửa được ngay, không cần duyệt lại. Chỉ minh chứng
        (bằng cấp, giấy tờ) do quản trị viên duyệt.
      </p>
      <div className="form-grid">
        {[
          ["displayName", "Tên hiển thị", "text", 120],
          ["province", "Tỉnh / thành phố", "text", 100],
          ["hourlyRate", "Học phí mỗi giờ (đ)", "number"],
          ["experience", "Số năm kinh nghiệm", "number"],
        ].map(([key, label, type, max]) => (
          <Field key={key} label={label}>
            <input
              type={type}
              required
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
      <Field label="Học lực, giải thưởng (tự khai, tùy chọn)">
        <textarea
          maxLength={1000}
          rows={3}
          value={form.achievements}
          onChange={(e) => set("achievements", e.target.value)}
        />
      </Field>
      <p className="muted">
        Phụ huynh thấy mục này với nhãn “Tự khai” (chưa xác minh). Nó không được
        dùng để xếp hạng gia sư.
      </p>
      <TimeSlots
        value={form.availability}
        onChange={(v) => set("availability", v)}
      />
      <button disabled={busy}>Lưu hồ sơ</button>
    </form>
  );
}
