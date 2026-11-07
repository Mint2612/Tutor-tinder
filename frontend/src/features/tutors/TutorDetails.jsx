import { Avatar, Availability } from "../../components/shared";
import { money, statusText } from "../../lib/constants";

export function TutorDetails({ tutor: t }) {
  return (
    <>
      <Avatar name={t.display_name} photo={t.photo_url} large />
      <h2>{t.display_name}</h2>
      <span className="status">{statusText[t.workflow_state]}</span>
      <p>
        {t.subjects.join(" · ")} · Lớp {t.grades.join(", ")}
      </p>
      <p>
        {money(t.hourly_rate)}/giờ · {t.province}
      </p>
      <h3>Học vấn & kinh nghiệm</h3>
      <p>
        {t.background} · {t.experience_years} năm kinh nghiệm
      </p>
      {t.achievements && (
        <>
          <h3>
            Học lực & giải thưởng <span className="status">Tự khai</span>
          </h3>
          <p>{t.achievements}</p>
        </>
      )}
      <h3>Cách tôi giảng dạy</h3>
      <p>{t.teaching_style}</p>
      <p>{t.teaching_description}</p>
      <h3>Lịch có thể sắp xếp</h3>
      <p>
        <Availability slots={t.availability} />
      </p>
    </>
  );
}
