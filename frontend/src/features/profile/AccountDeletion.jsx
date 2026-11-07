import { useState } from "react";
import { useWorkspace } from "../../hooks/useWorkspace";
import { api } from "../../services/api";
import { DeleteDialog } from "./DeleteDialog";

export function AccountDeletion() {
  const { user, busy, error, setError, run, clearWorkspace, setUser, navigate, setNotification } = useWorkspace();
  const [open, setOpen] = useState(false);
  if (!["PARENT", "TUTOR"].includes(user.role)) return null;
  return <section className="panel danger-zone">
    <h2>Xóa tài khoản</h2>
    <p>Xóa thông tin cá nhân và các hồ sơ của bạn, đóng các cặp liên quan và đăng xuất mọi thiết bị. Lịch sử được giữ dưới dạng ẩn danh. Thao tác không thể hoàn tác.</p>
    <button className="danger-button" disabled={busy} onClick={() => { setError(""); setOpen(true); }}>Xóa tài khoản của tôi</button>
    {open && <DeleteDialog title="Xóa tài khoản" passwordRequired busy={busy} error={error}
      description="Bạn sẽ mất quyền truy cập tài khoản, các hồ sơ con hoặc hồ sơ gia sư và các kết nối hiện tại. Hãy kiểm tra đúng tài khoản trước khi xác nhận."
      close={() => { setError(""); setOpen(false); }} confirm={(password) => run(async () => {
        await api("/account", { method: "DELETE", body: { password, confirmation: "DELETE" } });
        clearWorkspace(); setUser(null); navigate("home"); setNotification("Đã xóa tài khoản và đăng xuất mọi thiết bị.");
      })} />}
  </section>;
}
