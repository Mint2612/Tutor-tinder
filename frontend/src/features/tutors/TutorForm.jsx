import { useState } from "react";
import { findContact, contactMessage } from "../../lib/contactInfo";
import { Field, Choices } from "../../components/shared";
import { Combobox } from "../../components/Combobox";
import { TimeSlots } from "../../components/TimeSlots";
import { subjects, styles, blankTutor } from "../../lib/constants";
import { PROVINCES } from "../../lib/provinces";
import { parseSlot, toToken, slotProblems } from "../../lib/schedule";

export function TutorForm({ initial, save, busy }) {
  const [form, setForm] = useState(
    initial
      ? {
          // Legal name for the admin's identity check. The public display name is the account's
          // name (set when registering) and is edited under "Thông tin tài khoản".
          fullName: initial.full_name ?? "",
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
  // Phụ huynh đọc được các ô này; liên hệ chỉ được chia sẻ sau khi gia sư chấp nhận học thử.
  const contactProblem = ["methodology", "background", "style", "achievements"]
    .map((key) => contactMessage(findContact(form[key])))
    .find(Boolean);
  const set = (key, value) => {
    setValidationError("");
    setForm((f) => ({ ...f, [key]: value }));
  };
  const textField = ([key, label, type, max]) => (
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
  );
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
        if (contactProblem) {
          setValidationError(contactProblem);
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
        Lưu thay đổi sẽ đưa hồ sơ đã duyệt hoặc đang chờ duyệt về bản nháp: bạn
        cần gửi duyệt lại để tiếp tục hiện với phụ huynh.
      </p>
      <p className="muted">
        “Họ và tên” chỉ bạn và quản trị viên xem được, dùng để đối chiếu với giấy
        tờ. Phụ huynh chỉ thấy “Tên hiển thị” trong phần Thông tin tài khoản.
      </p>
      <div className="form-grid">
        {textField(["fullName", "Họ và tên (đúng như giấy tờ)", "text", 120])}
        <Combobox
          label="Tỉnh / thành phố"
          options={PROVINCES}
          value={form.province}
          onChange={(value) => set("province", value)}
          required
        />
        {textField(["hourlyRate", "Học phí mỗi giờ (đ)", "number"])}
        {textField(["experience", "Số năm kinh nghiệm", "number"])}
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
      {contactProblem && (
        <p className="field-error" role="alert">
          {contactProblem}
        </p>
      )}
      <p className="muted">
        Phụ huynh thấy mục này với nhãn “Tự khai” (chưa xác minh). Nó không được
        dùng để xếp hạng gia sư. Đừng ghi số điện thoại hoặc email trong các ô mô tả.
      </p>
      <TimeSlots
        value={form.availability}
        onChange={(v) => set("availability", v)}
      />
      <button disabled={busy}>Lưu hồ sơ</button>
    </form>
  );
}
