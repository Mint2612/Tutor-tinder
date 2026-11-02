# TutorMatch

Source chung cho nhóm: **Spring Boot + React JavaScript + MariaDB**. Dùng tài liệu MVP v0.4 trong `Specs/README.md` và `Specs/Requirement.md` làm cơ sở.

## Công cụ

| Phần | Công cụ |
| --- | --- |
| Backend | Java 21 (JDK), Spring Boot 4.1.2, Maven Wrapper |
| Frontend | React JavaScript, Vite, Node.js 24 (theo `.node-version` / `.nvmrc`), npm |
| Database | MariaDB 11.4 qua Docker Compose |
| Schema | Flyway SQL migration, Hibernate `validate` |
| IDE | IntelliJ IDEA cho Java hoặc VS Code + Java Extension Pack; DBeaver để xem DB |

Không cần cài Maven riêng: repo có `mvnw` và `mvnw.cmd`. Backend cần JDK, không chỉ JRE.

## Clone và tạo cấu hình

```sh
git clone https://github.com/Mint2612/Tutor-tinder.git
cd Tutor-tinder
cp .env.example .env
```

Windows PowerShell dùng `Copy-Item .env.example .env`. Chỉ commit `.env.example`, không commit `.env`.

## Cách 1: chạy toàn bộ bằng Docker

Chỉ cần Git và Docker Desktop đang chạy:

```sh
docker compose --profile app up --build -d
```

- Web: http://localhost:3000
- Backend: http://localhost:8080/api/health
- DB health: http://localhost:8080/actuator/health

Backend tự chạy Flyway và tạo 20 gia sư giả qua profile `demo`. Lần đầu tải image/dependency có thể mất vài phút. Nếu API chưa sẵn sàng ngay sau khi web lên, xem log và chờ backend khởi động:

```sh
docker compose logs -f backend
```

Dừng bằng `docker compose --profile app stop`. Cách này phù hợp chạy thử; khi sửa source, chạy lại lệnh `up --build`. Dùng cách 2 để hot reload frontend và debug Java.

## Cách 2: code trên máy / IDE

Cần Java 21, Node.js 24, npm và Docker Desktop. Nếu trước đó chạy cách 1, dừng backend/frontend Docker để tránh trùng cổng.

Từ thư mục gốc:

```sh
docker compose up -d --wait db
```

Terminal 1 — backend:

```sh
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
```

Windows dùng `./mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=demo"`.

Terminal 2 — frontend:

```sh
cd frontend
npm ci
npm run dev
```

Mở http://localhost:5173. Vite proxy `/api` về backend cổng 8080. Muốn chạy backend không seed: bỏ `-Dspring-boot.run.profiles=demo`. Trong IntelliJ, mở `backend/pom.xml`, chọn JDK 21, chạy `TutorMatchApplication`; đặt working directory là `backend/`, active profile `demo` nếu muốn seed.

Backend đọc `.env` gốc khi chạy từ thư mục gốc hoặc `backend/`. Biến môi trường có ưu tiên cao hơn file. Không cài thêm dotenv cho Java.

## Database

DBeaver kết nối `localhost:3306`, database/user/password theo `.env`. Nếu cổng bận, đổi `DB_PORT`; backend chạy local sẽ dùng giá trị mới. Nếu đổi cổng backend, frontend chạy local cần đặt `API_PROXY_TARGET` theo hướng dẫn ở `frontend/README.md`.

Schema ở `backend/src/main/resources/db/migration/V1__initial_schema.sql`. Đổi schema bằng migration mới `V2__ten_thay_doi.sql`; không sửa migration đã chạy, không bật Hibernate `ddl-auto=update`.

Seed chỉ chạy ở profile `demo`, chạy lại không nhân đôi. Các gia sư giả có trạng thái PENDING, chưa có embedding và không phải tài khoản đăng nhập. Không dùng profile demo với dữ liệu thật.

MariaDB lưu trong volume `mariadb_data`. `docker compose stop` giữ dữ liệu. **`docker compose down -v` xóa volume và toàn bộ dữ liệu local.** Đổi mật khẩu `.env` không đổi mật khẩu của DB đã được khởi tạo; cần đổi trong MariaDB hoặc tạo lại DB local nếu có thể bỏ dữ liệu.

## Kiểm tra trước PR

Frontend:

```sh
cd frontend
npm ci
npm run lint
npm run build
```

Backend có unit test cosine và integration test Flyway/JPA/seed. Cách dễ nhất là dùng MariaDB test riêng trong Docker, không tác động DB phát triển:

```sh
# Chạy từ thư mục gốc
docker compose --profile test run --build --rm backend-test
docker compose --profile test stop test-db
```

Nếu chạy `cd backend && ./mvnw verify` bằng JDK trên máy, phải cung cấp database test riêng bằng biến môi trường `DB_HOST`, `DB_PORT`, `DATABASE_NAME`, `DATABASE_USER`, `DATABASE_PASSWORD`. Không chạy integration test trên DB thật: test bật seed demo. CI cung cấp MariaDB test riêng và chạy `./mvnw -B verify`.

## Cấu trúc

```text
backend/
  pom.xml, mvnw, mvnw.cmd        Maven Wrapper
  src/main/java/com/tutormatch/  Application, controller, entity/repository mẫu, matching, seed
  src/main/resources/           application.yaml và Flyway migrations
  src/test/                     Unit và integration tests
frontend/
  src/                          React components và API helper
  vite.config.js                Proxy API khi phát triển
  nginx.conf                    SPA routing và proxy khi chạy Docker
compose.yaml                    MariaDB + app + test profiles
.github/workflows/ci.yml         Kiểm tra frontend và backend
Specs/                          Tài liệu sản phẩm gốc, giữ nguyên
docs/architecture.md            Quy ước và việc tiếp theo
```

## Làm việc nhóm

1. Pull `main`, tạo nhánh `feat/ten-tinh-nang` hoặc `fix/ten-loi`.
2. Frontend dùng npm, commit `frontend/package-lock.json`; chạy `npm ci` sau khi pull dependency mới.
3. Backend dùng Maven Wrapper; version dependency được Spring Boot quản lý.
4. Đổi DB phải có Flyway migration mới. Backend tự áp dụng migration lúc khởi động.
5. Chạy kiểm tra trước PR, nhờ thành viên khác review. Maintainer nên bật branch protection và yêu cầu CI pass.

## Đã có / chưa có

Đã có trang giới thiệu responsive, frontend API helper, REST health, Actuator health có kiểm tra DB, schema nền tảng, entity/repository User mẫu, seed, cosine utility, unit/integration tests và GitHub Actions.

Chưa có đăng nhập/Spring Security, CRUD hồ sơ, API matching, AI provider, học thử, đánh giá hay trang admin. Không có API công khai đọc hồ sơ trong bộ khung. Không đưa API key vào biến `VITE_*`; chúng được nhúng vào frontend. Dùng dữ liệu giả trong phát triển.
