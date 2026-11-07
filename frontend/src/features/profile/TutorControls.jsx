import { useState } from "react";
import { useWorkspace } from "../../hooks/useWorkspace";
import { api } from "../../services/api";
import { DeleteDialog } from "./DeleteDialog";

export function TutorControls() {
  const { tutor, setTutor, busy, error, setError, run, setDocuments, setNotification } = useWorkspace();
  const [deleting, setDeleting] = useState(false);
  const [pendingAcceptance, setPendingAcceptance] = useState(null);
  if (!tutor) return null;
  return <section className="panel tutor-controls">
    <h2>Trạng thái nhận học sinh</h2>
    <p role="status">{tutor.accepting_students ? "Đang bật nhận học sinh" : "Đã tạm ẩn khỏi danh sách đề xuất"}</p>
    <p>Tạm ẩn giúp ngừng nhận đề xuất mới và giữ các Match đã có. Bật lại chỉ có hiệu lực khi hồ sơ được duyệt và admin cho phép hiển thị.</p>
    <label className="consent">
      <input type="checkbox" role="switch" aria-label="Nhận học sinh mới" checked={pendingAcceptance ?? !!tutor.accepting_students} disabled={busy}
        onChange={(e) => { const acceptingStudents = e.target.checked; setPendingAcceptance(acceptingStudents); run(async () => {
          setTutor(await api("/tutor/availability", { method: "PUT", body: { acceptingStudents } }));
          setNotification(acceptingStudents ? "Đã bật nhận học sinh. Quyền hiển thị vẫn phụ thuộc kết quả duyệt và admin." : "Đã tạm ẩn khỏi đề xuất. Các Match đã có được giữ lại.");
        }).finally(() => setPendingAcceptance(null)); }} />
      <span>Nhận học sinh mới</span>
    </label>
    {!tutor.visible && <p className="muted">Admin đang tắt hiển thị hồ sơ. Bật công tắc này không thay đổi quyết định của admin.</p>}
    <hr />
    <h3>Xóa hồ sơ giảng dạy</h3>
    <p>Gỡ thông tin, ảnh và minh chứng của hồ sơ; đóng các cặp liên quan. Tài khoản vẫn dùng được. Hồ sơ tạo lại phải được duyệt lại.</p>
    <button className="danger-button" disabled={busy} onClick={() => { setError(""); setDeleting(true); }}>Xóa hồ sơ giảng dạy</button>
    {deleting && <DeleteDialog title="Xóa hồ sơ giảng dạy" passwordRequired busy={busy} error={error}
      description="Thông tin, ảnh và giấy tờ của hồ sơ sẽ bị xóa; các cặp liên quan được đóng. Lịch sử được giữ dưới dạng ẩn danh."
      close={() => { setError(""); setDeleting(false); }} confirm={(password) => run(async () => {
        await api("/tutor/profile", { method: "DELETE", body: { password, confirmation: "DELETE" } });
        setDeleting(false); setTutor(null); setDocuments([]); setNotification("Đã xóa hồ sơ giảng dạy. Bạn có thể tạo hồ sơ mới và gửi duyệt lại.");
      })} />}
  </section>;
}
