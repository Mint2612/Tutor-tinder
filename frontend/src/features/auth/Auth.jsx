import { useState } from "react";
import { api } from "../../services/api";
import { Field, Notice } from "../../components/shared";
import { phonePattern, phoneHint } from "../../lib/phone";

export function Auth({
  page,
  initialValues = {},
  setPage,
  run,
  busy,
  onLogin,
  notify,
  onReset,
}) {
  const [message, setMessage] = useState("");
  return (
    <section className="auth-layout">
      <div>
        <p className="eyebrow">CHÀO MỪNG ĐẾN TUTORMATCH</p>
        <h1>
          Mỗi hành trình tốt
          <br />
          bắt đầu từ <em>kết nối.</em>
        </h1>
        <p className="intro">
          Một không gian cho phụ huynh và gia sư cùng đồng hành với việc học của
          con.
        </p>
      </div>
      <div className="panel auth">
        <h2>
          {
            {
              login: "Chào bạn trở lại",
              register: "Tạo tài khoản",
              verify: "Xác minh tài khoản",
              forgot: "Quên mật khẩu",
              reset: "Đặt lại mật khẩu",
            }[page]
          }
        </h2>
        <Notice>{message}</Notice>
        <form
          key={page}
          onSubmit={(e) => {
            e.preventDefault();
            const values = Object.fromEntries(new FormData(e.currentTarget));
            run(async () => {
              const result = await api(`/auth/${page}`, {
                method: "POST",
                body: values,
              });
              if (page === "login") onLogin(result);
              else if (page === "register" && values.role === "PARENT")
                // Parents need no verification: sign in straight away with the password just chosen.
                onLogin(
                  await api("/auth/login", {
                    method: "POST",
                    body: { email: values.email, password: values.password },
                  }),
                );
              else {
                setMessage(result.message);
                notify(result.message);
                if (page === "register") setPage("verify");
                if (page === "verify") setPage("login");
                if (page === "reset") {
                  onReset();
                  notify(result.message);
                  setPage("login");
                }
              }
            });
          }}
        >
          {["register", "login", "forgot"].includes(page) && (
            <Field label="Email">
              <input
                name="email"
                type="email"
                required
                maxLength={255}
                autoComplete="email"
              />
            </Field>
          )}
          {page === "register" && (
            <>
              <Field label="Tên hiển thị">
                <input
                  name="name"
                  required
                  maxLength={120}
                  autoComplete="name"
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
                />
              </Field>
              <Field label="Bạn tham gia với vai trò">
                <select name="role">
                  <option value="PARENT">Phụ huynh</option>
                  <option value="TUTOR">Gia sư</option>
                </select>
              </Field>
              <p className="muted">
                Phụ huynh dùng được ngay sau khi đăng ký. Gia sư cần xác minh
                email qua liên kết gửi đến hộp thư.
              </p>
            </>
          )}
          {["register", "login", "reset"].includes(page) && (
            <Field
              label={
                page === "login"
                  ? "Mật khẩu"
                  : page === "register"
                    ? "Mật khẩu (12–72 ký tự)"
                    : "Mật khẩu mới (12–72 ký tự)"
              }
            >
              <input
                name="password"
                type="password"
                required
                minLength={page === "login" ? 1 : 12}
                maxLength={72}
                autoComplete={
                  page === "login" ? "current-password" : "new-password"
                }
              />
            </Field>
          )}
          {["verify", "reset"].includes(page) && (
            <>
              <p className="muted">
                Mở liên kết trong email để điền sẵn thông tin, hoặc nhập mã
                trong email. Mã có hiệu lực trong 10 phút. Số điện thoại chỉ
                dùng để liên hệ sau khi chấp nhận học thử.
              </p>
              {initialValues.challengeId && initialValues.code ? (
                <>
                  <input
                    type="hidden"
                    name="challengeId"
                    value={initialValues.challengeId}
                  />
                  <input type="hidden" name="code" value={initialValues.code} />
                  <Notice>
                    Đã nhận liên kết từ email.{" "}
                    {page === "verify"
                      ? "Nhấn Xác minh để kích hoạt tài khoản."
                      : "Nhập mật khẩu mới để tiếp tục."}
                  </Notice>
                </>
              ) : (
                <>
                  <Field label="Mã yêu cầu trong email">
                    <input
                      name="challengeId"
                      required
                      maxLength={36}
                      defaultValue={initialValues.challengeId || ""}
                    />
                  </Field>
                  <Field label="Mã xác thực trong email">
                    <input
                      name="code"
                      required
                      maxLength={100}
                      autoComplete="one-time-code"
                      defaultValue={initialValues.code || ""}
                    />
                  </Field>
                </>
              )}
            </>
          )}
          <button disabled={busy} className="full">
            {
              {
                login: "Đăng nhập →",
                register: "Tạo tài khoản →",
                verify: "Xác minh",
                forgot: "Gửi hướng dẫn đặt lại",
                reset: "Lưu mật khẩu mới",
              }[page]
            }
          </button>
        </form>
        <div className="auth-links">
          {page !== "login" && (
            <button className="text-button" onClick={() => setPage("login")}>
              Đăng nhập
            </button>
          )}
          {page !== "register" && (
            <button className="text-button" onClick={() => setPage("register")}>
              Tạo tài khoản
            </button>
          )}
          {page === "login" && (
            <button className="text-button" onClick={() => setPage("forgot")}>
              Quên mật khẩu?
            </button>
          )}
          {page !== "verify" && (
            <button className="text-button" onClick={() => setPage("verify")}>
              Xác minh email
            </button>
          )}
          {page === "forgot" && (
            <button className="text-button" onClick={() => setPage("reset")}>
              Tôi đã có mã đặt lại
            </button>
          )}
        </div>
        {page === "verify" && (
          <form
            onSubmit={(e) => {
              e.preventDefault();
              const body = Object.fromEntries(new FormData(e.currentTarget));
              run(async () =>
                setMessage(
                  (await api("/auth/resend", { method: "POST", body })).message,
                ),
              );
            }}
          >
            <Field label="Gửi lại hướng dẫn đến tài khoản">
              <input type="email" name="email" required />
            </Field>
            <button className="secondary" disabled={busy}>
              Gửi lại mã
            </button>
          </form>
        )}
      </div>
    </section>
  );
}
