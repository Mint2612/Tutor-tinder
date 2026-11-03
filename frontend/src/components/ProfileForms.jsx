import { useState } from "react";
import { Field, Choices, Schedule } from "./shared";
import { subjects, questions, styles, blankTutor } from "../lib/constants";

export function StudentForm({ initial, save, cancel, busy }) {
  const [form, setForm] = useState(initial),
    set = (key, value) => setForm((f) => ({ ...f, [key]: value }));
  return (
    <form
      className="panel form-panel"
      onSubmit={(e) => {
        e.preventDefault();
        save(form);
      }}
    >
      <h2>Hồ sơ & cách con học</h2>
      <p className="muted">
        6 câu hỏi · khoảng 5 phút. Không cần cung cấp tên đầy đủ hay thông tin
        nhạy cảm.
      </p>
      <div className="form-grid">
        <Field label="Tên gọi của con">
          <input
            required
            maxLength={120}
            value={form.nickname}
            onChange={(e) => set("nickname", e.target.value)}
          />
        </Field>
        <Field label="Lớp">
          <input
            type="number"
            min={1}
            max={12}
            required
            value={form.grade}
            onChange={(e) => set("grade", +e.target.value)}
          />
        </Field>
        <Field label="Tỉnh / thành phố">
          <input
            required
            maxLength={100}
            value={form.province}
            onChange={(e) => set("province", e.target.value)}
          />
        </Field>
        <Field label="Hình thức học">
          <select
            value={form.mode}
            onChange={(e) => set("mode", e.target.value)}
          >
            <option value="ONLINE">Trực tuyến</option>
            <option value="IN_PERSON">Trực tiếp</option>
          </select>
        </Field>
        <Field label="Ngân sách tối đa mỗi giờ (đ)">
          <input
            type="number"
            min={1}
            max={10000000}
            required
            value={form.budget}
            onChange={(e) => set("budget", +e.target.value)}
          />
        </Field>
      </div>
      <fieldset>
        <legend>Môn cần học</legend>
        <Choices
          options={subjects}
          value={form.subjects}
          onChange={(value) => set("subjects", value)}
        />
      </fieldset>
      <Field label="Mục tiêu, khó khăn và điều bạn muốn gia sư hiểu">
        <textarea
          required
          maxLength={3000}
          rows={3}
          value={form.needs}
          onChange={(e) => set("needs", e.target.value)}
        />
      </Field>
      <div className="survey">
        <h3>Khảo sát cách học</h3>
        {questions.map(([id, label, options], i) => (
          <Field key={id} label={`${i + 1}. ${label}`}>
            <select
              required
              value={form.survey[id] || ""}
              onChange={(e) =>
                set("survey", { ...form.survey, [id]: e.target.value })
              }
            >
              <option value="">Chọn câu trả lời</option>
              {options.map(([key, text]) => (
                <option key={key} value={key}>
                  {text}
                </option>
              ))}
            </select>
          </Field>
        ))}
      </div>
      <Schedule
        value={form.availability}
        onChange={(value) => set("availability", value)}
      />
      <label className="consent">
        <input
          type="checkbox"
          required
          checked={form.consent}
          onChange={(e) => set("consent", e.target.checked)}
        />
        <span>
          Tôi là phụ huynh / người giám hộ và đồng ý lưu thông tin này để tìm
          gia sư. Chỉ các đặc điểm học tập đã ẩn danh được gửi đến dịch vụ AI.
        </span>
      </label>
      <div className="row">
        <button disabled={busy}>Lưu hồ sơ & khảo sát</button>
        <button type="button" className="secondary" onClick={cancel}>
          Hủy
        </button>
      </div>
    </form>
  );
}

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
          background: initial.background,
          experience: initial.experience_years,
          availability: initial.availability,
        }
      : blankTutor,
  );
  const set = (key, value) => setForm((f) => ({ ...f, [key]: value }));
  return (
    <form
      className="panel form-panel"
      onSubmit={(e) => {
        e.preventDefault();
        save(form);
      }}
    >
      <h2>Hồ sơ giảng dạy</h2>
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
          value={form.modes}
          onChange={(v) => set("modes", v)}
        />
        <small>ONLINE: trực tuyến · IN_PERSON: trực tiếp</small>
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
