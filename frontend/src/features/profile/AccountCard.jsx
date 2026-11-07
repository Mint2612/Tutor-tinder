import { useWorkspace } from "../../hooks/useWorkspace";
import { api } from "../../services/api";
import { Field } from "../../components/shared";

const roleText = { PARENT: "Phụ huynh", TUTOR: "Gia sư", ADMIN: "Quản trị viên" };

export function AccountCard() {
  const { account, setAccount, user, setUser, busy, run, setNotification } =
    useWorkspace();
  if (!account) return null;
  return (
    <form
      // Remount after a save or the first load so the inputs show the stored values.
      key={`${account.name}|${account.phone}`}
      className="panel"
      onSubmit={(e) => {
        e.preventDefault();
        const values = Object.fromEntries(new FormData(e.currentTarget));
        run(async () => {
          const saved = await api("/account", { method: "PUT", body: values });
          setAccount(saved);
          setUser({ ...user, name: saved.name });
          setNotification("Đã cập nhật thông tin tài khoản.");
        });
      }}
    >
      <h2>Thông tin tài khoản</h2>
      <div className="form-grid">
        <Field label="Họ và tên">
          <input
            name="name"
            required
            maxLength={120}
            autoComplete="name"
            defaultValue={account.name}
          />
        </Field>
        <Field label="Số điện thoại (ví dụ +84901234567)">
          <input
            name="phone"
            type="tel"
            required
            pattern="\+[1-9][0-9]{7,14}"
            autoComplete="tel"
            defaultValue={account.phone ?? ""}
          />
        </Field>
        <Field label="Email (không đổi được)">
          <input value={account.email} readOnly />
        </Field>
        <Field label="Vai trò">
          <input value={roleText[account.role] ?? account.role} readOnly />
        </Field>
      </div>
      <p className="muted">
        Số điện thoại chỉ được chia sẻ sau khi gia sư chấp nhận học thử.
      </p>
      <button disabled={busy}>Lưu thông tin</button>
    </form>
  );
}
