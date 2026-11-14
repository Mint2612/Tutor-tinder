import { useWorkspace } from "../../hooks/useWorkspace";
import { api } from "../../services/api";
import { Field } from "../../components/shared";
import { phonePattern, phoneHint } from "../../lib/phone";

const roleText = { PARENT: "Phụ huynh", TUTOR: "Gia sư", ADMIN: "Quản trị viên" };

export function AccountCard() {
  const {
    account,
    setAccount,
    user,
    setUser,
    tutor,
    setTutor,
    busy,
    run,
    setNotification,
  } = useWorkspace();
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
          // A tutor's public name follows the display name, so reload the profile that carries it.
          if (tutor) setTutor(await api("/tutor/profile"));
          setNotification("Đã cập nhật thông tin tài khoản.");
        });
      }}
    >
      <h2>Thông tin tài khoản</h2>
      <div className="form-grid">
        <Field label="Tên hiển thị">
          <input
            name="name"
            required
            maxLength={120}
            autoComplete="name"
            defaultValue={account.name}
          />
        </Field>
        <Field label="Số điện thoại">
          <input
            name="phone"
            type="tel"
            required
            pattern={phonePattern}
            placeholder="0901234567 hoặc +84901234567"
            title={phoneHint}
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
        {account.role === "TUTOR"
          ? "Tên hiển thị là tên phụ huynh thấy trên hồ sơ gia sư của bạn (mặc định là tên lúc đăng ký). "
          : "Tên hiển thị là tên bạn dùng trên TutorMatch (mặc định là tên lúc đăng ký). "}
        Số điện thoại chỉ được chia sẻ sau khi gia sư chấp nhận học thử.
      </p>
      <button disabled={busy}>Lưu thông tin</button>
    </form>
  );
}
