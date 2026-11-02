# Frontend

React JavaScript + Vite. Xem hướng dẫn chạy tại [README gốc](../README.md).

- `npm ci`: cài đúng dependency từ lockfile.
- `npm run dev`: http://localhost:5173; `/api` proxy về http://localhost:8080.
- `npm run lint`: Oxlint theo template Vite chính thức.
- `npm run build`: xuất `dist/`.
- `npm run preview`: xem bản build UI; không có API proxy, dùng dev server hoặc Docker để thử API.

Có thể đổi proxy lúc chạy: `API_PROXY_TARGET=http://localhost:8081 npm run dev`. Windows PowerShell đặt `$env:API_PROXY_TARGET="http://localhost:8081"` trước khi chạy lệnh.

Thêm component trong `src/components/`, màn hình trong `src/pages/`, API client trong `src/lib/`. Không đưa secrets hoặc AI key vào frontend.
