# Environment setup

Copy `.env.example` to `.env`. Keep backend credentials out of frontend variables and version control. The native backend imports root `.env`; Compose supplies variables explicitly.

| Variable | Purpose / local default |
|---|---|
| DATABASE_NAME / DATABASE_USER / DATABASE_PASSWORD | Application MariaDB schema and credentials |
| DATABASE_ROOT_PASSWORD | Local database provisioning only; not passed to backend |
| DB_HOST / DB_PORT | Native database connection; Docker backend uses db:3306 |
| BACKEND_PORT / FRONTEND_PORT | Exposed local ports, default 8080 / 3000 |
| COOKIE_SECURE | false for local HTTP; true with HTTPS |
| APP_PUBLIC_URL | Public frontend origin used in verification/reset email links; default http://localhost:3000 |
| MAIL_HOST / MAIL_PORT | SMTP host/port; Compose mailpit:1025; native localhost:1025 |
| MAIL_USERNAME / MAIL_PASSWORD | External SMTP credentials; empty for local Mailpit |
| MAIL_FROM | Sender address; local noreply@tutormatch.local |
| MAIL_AUTH | Enable SMTP authentication when required by the provider |
| MAIL_STARTTLS | Enable and require STARTTLS |
| MAIL_SSL | Enable implicit SMTP TLS when required by the provider |
| MAILPIT_UI_PORT / MAILPIT_SMTP_PORT | Local inbox/SMTP ports, default 8025 / 1025 |
| LLM_URL | Full chat-completions endpoint; empty while AI is deferred |
| LLM_API_KEY | Empty for local Ollama; optional backend key for other endpoints |
| LLM_MODEL | empty while AI is deferred |
| ADMIN_EMAIL / ADMIN_PASSWORD | Private first-start admin provisioning; remove afterward |

SMTP connection/read/write timeouts are bounded. Authentication requires verified email; SMS is not used. Phone numbers remain required as contact details. The old `DELIVERY_URL`, `DELIVERY_KEY` and `AI_API_KEY` variables are no longer used.

Current scope is Sprints 1–4. Leave `LLM_URL`, `LLM_API_KEY` and `LLM_MODEL` empty. Recommendations use deterministic rules; AI setup and model downloads are deferred.

Start with `docker compose --profile app up --build -d`.

For native development: set `MAIL_HOST=localhost`, `APP_PUBLIC_URL=http://localhost:5173`. Start MariaDB/Mailpit with Compose; run `./mvnw spring-boot:run` under `backend/` and `npm ci && npm run dev` under `frontend/`. Vite proxies `/api` to localhost:8080; override `API_PROXY_TARGET` if needed.

For real outbound email, configure the SMTP provider's host, port, username/password, approved sender and required TLS settings. Mailpit only captures development messages. See [Mailpit configuration](https://mailpit.axllent.org/docs/configuration/).

## Gmail SMTP

Email xác minh đăng ký và đặt lại mật khẩu dùng chung SMTP, nên không cần thay đổi controller/service. Trong `.env`, đặt:

```dotenv
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=youraccount@gmail.com
MAIL_PASSWORD=
MAIL_FROM=youraccount@gmail.com
MAIL_AUTH=true
MAIL_STARTTLS=true
MAIL_SSL=false
```

1. Đăng nhập tài khoản gửi, bật **2-Step Verification**, rồi mở [App Passwords](https://myaccount.google.com/apppasswords), tạo mật khẩu ứng dụng tên `TutorMatch`. Theo [hướng dẫn Google](https://support.google.com/accounts/answer/185833), App Password chỉ dùng được khi đã bật xác minh hai bước; một số chính sách tài khoản có thể không cho tạo.
2. Điền App Password vào `MAIL_PASSWORD` trong `.env`, bỏ khoảng trắng phân nhóm. Không dùng mật khẩu đăng nhập Gmail; không gửi secret vào chat hoặc commit lên GitHub.
3. Áp dụng cấu hình: `docker compose --profile app up -d --no-deps backend`. Nếu chạy backend trực tiếp bằng Maven, khởi động lại backend.
4. Mở ứng dụng, đăng ký bằng địa chỉ email thật có thể nhận thư. Kiểm tra Gmail **Inbox/Spam**, mở link xác minh rồi đăng nhập. Thử **Quên mật khẩu** và kiểm tra thư đặt lại mật khẩu tương tự. Thư mới gửi qua Gmail sẽ không xuất hiện trong Mailpit.

`APP_PUBLIC_URL=http://localhost:3000` dành cho mở link trên chính máy chạy ứng dụng; nếu chạy Vite trực tiếp, dùng `http://localhost:5173`. Địa chỉ này cần truy cập được từ thiết bị mở email.

Nếu gửi thư trả HTTP 503, kiểm tra App Password, tài khoản gửi và kết nối SMTP. Chưa điền `MAIL_PASSWORD` thì chưa thể kiểm thử gửi Gmail thật. Để quay lại Mailpit: host `mailpit`, port `1025`, username/password trống, from `noreply@tutormatch.local`, auth/starttls/ssl đều `false`, rồi khởi động lại backend.

For public hosting: configure HTTPS, secure cookies, trusted reverse proxy headers and private backend/database access. Restrict evidence access and configure encrypted storage/backups and retention. Keep administrator bootstrap credentials in a secret manager; startup creates only a new admin and never changes an existing account's role or password. Hosting and an external SMTP account are outside the local runtime configuration.
