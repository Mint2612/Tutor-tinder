import { useWorkspace } from "../hooks/useWorkspace";
import { ParentProfile } from "../features/profile/ParentProfile";
import { TutorProfile } from "../features/profile/TutorProfile";

// One profile page, two views: parents manage their account, tutors also manage their public
// teaching profile, photo, verification badges and evidence.
export function ProfilePage() {
  const { user } = useWorkspace();
  return user.role === "TUTOR" ? <TutorProfile /> : <ParentProfile />;
}
