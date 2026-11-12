import { useState } from "react";
import { Field, Choices } from "../../components/shared";
import { Combobox } from "../../components/Combobox";
import { TimeSlots } from "../../components/TimeSlots";
import { subjects, questions } from "../../lib/constants";
import { PROVINCES } from "../../lib/provinces";
import { parseSlot, toToken, slotProblems } from "../../lib/schedule";
import { findContact, contactMessage } from "../../lib/contactInfo";

export function StudentForm({ initial, save, cancel, busy }) {
  // Free time as rows { day, start, end }; older coarse blocks open as their equivalent range.
  const [form, setForm] = useState({
    ...initial,
    availability: initial.availability.map(parseSlot),
  });
  const [validationError, setValidationError] = useState("");
  // Tutors can read this text, but contact details only go out after a trial lesson is accepted.
  const contactProblem = contactMessage(findContact(form.needs));
  const set = (key, value) => {
    setValidationError("");
    setForm((f) => ({ ...f, [key]: value }));
  };
  return (
    <form
      className="panel form-panel"
      onSubmit={(e) => {
        e.preventDefault();
        if (!form.subjects.length || !form.availability.length) {
          setValidationError(
            "Vui lòng chọn ít nhất một môn học và thêm ít nhất một khung giờ rảnh.",
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
      <h2>Hồ sơ & cách con học</h2>
      {validationError && (
        <div className="error" role="alert">
          {validationError}
        </div>
      )}
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
        <Combobox
          label="Tỉnh / thành phố"
          options={PROVINCES}
          value={form.province}
          onChange={(value) => set("province", value)}
          required
        />
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
          aria-invalid={contactProblem ? "true" : undefined}
          onChange={(e) => set("needs", e.target.value)}
        />
      </Field>
      {contactProblem ? (
        <p className="field-error" role="alert">
          {contactProblem}
        </p>
      ) : (
        <p className="muted">
          Đừng ghi số điện thoại hoặc email ở đây: thông tin liên hệ chỉ được chia
          sẻ sau khi gia sư chấp nhận học thử.
        </p>
      )}
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
      <TimeSlots
        value={form.availability}
        onChange={(value) => set("availability", value)}
        title="Thời gian rảnh của con mỗi tuần"
        hint="Thêm từng khung giờ con có thể học. Gia sư được gợi ý khi có giờ rảnh trùng ít nhất 1 giờ với một khung bên dưới (khung ngắn hơn 1 giờ thì cần trùng cả khung)."
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
          gia sư. Tên, trường và thông tin liên hệ của con không được chia sẻ cho
          gia sư trước khi hai bên cùng OK và Match.
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
