import { useState } from "react";
import { api } from "../lib/api";
import { Field, Notice } from "./shared";

export function Auth({ page, setPage, run, busy, onLogin, notify }) {
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
              else {
                setMessage(result.message);
                notify(result.message);
                if (page === "register") setPage("verify");
                if (page === "reset") setPage("login");
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
              <Field label="Họ và tên">
                <input
                  name="name"
                  required
                  maxLength={120}
                  autoComplete="name"
                />
              </Field>
              <Field label="Số điện thoại (ví dụ +84901234567)">
                <input
                  name="phone"
                  type="tel"
                  required
                  pattern="\+[1-9][0-9]{7,14}"
                  autoComplete="tel"
                />
              </Field>
              <Field label="Bạn tham gia với vai trò">
                <select name="role">
                  <option value="PARENT">Phụ huynh</option>
                  <option value="TUTOR">Gia sư</option>
                </select>
              </Field>
            </>
          )}
          {["register", "login", "reset"].includes(page) && (
            <Field
              label={
                page === "login" ? "Mật khẩu" : "Mật khẩu mới (12–72 ký tự)"
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
                Nhập mã định danh yêu cầu và mã bí mật được gửi trong email hoặc
                SMS. Email và điện thoại có hai yêu cầu xác minh riêng.
              </p>
              <Field label="Mã định danh yêu cầu (challenge ID)">
                <input name="challengeId" required maxLength={36} />
              </Field>
              <Field label="Mã bí mật">
                <input
                  name="code"
                  required
                  maxLength={100}
                  autoComplete="one-time-code"
                />
              </Field>
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
              Xác minh email / điện thoại
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
