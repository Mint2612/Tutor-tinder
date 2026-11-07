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

For public hosting: configure HTTPS, secure cookies, trusted reverse proxy headers and private backend/database access. Restrict evidence access and configure encrypted storage/backups and retention. Keep administrator bootstrap credentials in a secret manager; startup creates only a new admin and never changes an existing account's role or password. Hosting and an external SMTP account are outside the local runtime configuration.
