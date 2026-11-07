import { useWorkspace } from "../../hooks/useWorkspace";
import { api } from "../../services/api";
import { statusText } from "../../lib/constants";
import { PageHeading } from "../../components/shared";
import { TutorForm } from "../tutors/TutorForm";
import { EvidenceSection } from "../tutors/EvidenceSection";
import { AccountCard } from "./AccountCard";
import { AvatarUploader } from "./AvatarUploader";
import { VerificationBadges } from "./VerificationBadges";

export function TutorProfile() {
  const { busy, tutor, setTutor, setNotification, run } = useWorkspace();
  return (
    <>
      <PageHeading
        eyebrow="KHÔNG GIAN GIA SƯ"
        title="Hồ sơ của tôi."
        description="Cập nhật thông tin và ảnh bất cứ lúc nào. Quản trị viên chỉ duyệt phần minh chứng trước khi bạn xuất hiện với phụ huynh."
      />
      <section className="panel profile-head">
        <AvatarUploader />
        {tutor && (
          <div className="profile-status">
            <span className="status">{statusText[tutor.workflow_state]}</span>
            {tutor.rejection_reason && <p>{tutor.rejection_reason}</p>}
          </div>
        )}
      </section>
      <VerificationBadges tutor={tutor} />
      <TutorForm
        // Keyed by profile, not revision: uploading a photo must not discard unsaved edits.
        key={tutor?.id ?? "new"}
        initial={tutor}
        busy={busy}
        save={(form) =>
          run(async () => {
            setTutor(await api("/tutor/profile", { method: "PUT", body: form }));
            setNotification("Đã lưu thay đổi hồ sơ.");
          })
        }
      />
      {tutor && <EvidenceSection />}
      <AccountCard />
    </>
  );
}
