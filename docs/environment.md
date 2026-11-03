# Environment setup

Use `.env.example` as the configuration template. Never commit `.env` or put backend keys in VITE-prefixed variables. The backend imports root `.env` for native development; Docker Compose explicitly supplies credentials to its services.

| Variable | Purpose |
|---|---|
| DATABASE_NAME / DATABASE_USER / DATABASE_PASSWORD | MariaDB schema and application credentials |
| DATABASE_ROOT_PASSWORD | Local Compose database provisioning; not passed to backend |
| DB_HOST / DB_PORT | Native DB connection; Docker backend uses db:3306 |
| BACKEND_PORT / FRONTEND_PORT | Local exposed ports, defaults 8080/3000 |
| COOKIE_SECURE | true by default in backend; false only for local HTTP Compose |
| DELIVERY_URL / DELIVERY_KEY | Trusted HTTP webhook that delivers real EMAIL and SMS challenges; mandatory for account activation/reset |
| LLM_URL / LLM_API_KEY / LLM_MODEL | Optional full chat-completions-compatible endpoint/key/model; absence uses deterministic fallback |
| ADMIN_EMAIL / ADMIN_PASSWORD | First startup admin provisioning; password 16–72 characters and <=72 UTF-8 bytes; remove after creation |

Keep admin email canonical lowercase. The bootstrap creates only a new ADMIN and never upgrades an existing account. Store the initial password in a deployment secret manager, then remove it from runtime configuration after provisioning. Administrators are provisioned through the deployment trust boundary rather than public channel verification.

Native: Java 21, Node 24 (see `.node-version`), npm lockfile, MariaDB 11.4. Backend: `cd backend && ./mvnw spring-boot:run`. Frontend: `cd frontend && npm ci && npm run dev`. The Vite proxy and Docker nginx both use relative `/api` URLs. Backend secrets never enter the frontend bundle.

Production: terminate HTTPS at a trusted reverse proxy, enable secure cookies, keep backend/private DB ports restricted, and have the proxy replace client-supplied forwarding headers. Spring uses framework forwarded-header handling; do not expose it directly to an untrusted network. Nginx overwrites X-Forwarded-For for accurate IP throttling. Configure TLS/encryption and retention for database backups containing identity evidence. Requests/uploads are limited at both nginx and Spring. Delivery adapter must authenticate backend calls and avoid logging secrets; use HTTPS. AI endpoint receives only allowlisted learning attributes.

The current application has no built-in email/SMS sender credentials, no live provider key, no infrastructure automation for HTTPS or backup retention, and no hosted deployment in this change. Set those values through your chosen provider without changing the stack.
