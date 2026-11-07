# Frontend

React JavaScript + Vite. Xem hướng dẫn chạy tại [README gốc](../README.md).

- `npm ci`: cài đúng dependency từ lockfile.
- `npm run dev`: http://localhost:5173; `/api` proxy về http://localhost:8080.
- `npm run lint`: Oxlint theo template Vite chính thức.
- `npm run build`: xuất `dist/`.
- `npm run preview`: xem bản build UI; không có API proxy, dùng dev server hoặc Docker để thử API.

Có thể đổi proxy lúc chạy: `API_PROXY_TARGET=http://localhost:8081 npm run dev`. Windows PowerShell đặt `$env:API_PROXY_TARGET="http://localhost:8081"` trước khi chạy lệnh.

Thêm component dùng chung trong `src/components/`, component theo feature trong `src/features/`, màn hình trong `src/pages/`, API client trong `src/services/`. Không đưa secrets hoặc AI key vào frontend.

Điều phối nằm trong `src/app/`; hook dùng chung nằm trong `src/hooks/`. Hash routing hỗ trợ refresh, back và liên kết xác thực/đặt lại mật khẩu từ email. `npm run test:e2e` chạy Playwright; xem [hướng dẫn kiểm thử](../docs/verification.md).
