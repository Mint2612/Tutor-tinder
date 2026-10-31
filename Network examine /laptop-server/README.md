# Demo CRUD: Sổ sinh viên (React + Express)

## Chạy
```bash
npm install
npm run dev
```
Terminal sẽ in ra dòng `Mở trên điện thoại: http://192.168.x.x:5173`.
Điện thoại và laptop phải **cùng WiFi**, rồi mở link đó trên điện thoại.

## Nếu điện thoại không vào được
1. Windows: đặt WiFi sang **Private** và mở firewall:
   `New-NetFirewallRule -DisplayName "Demo" -Direction Inbound -Protocol TCP -LocalPort 5173 -Action Allow`
   (PowerShell chạy Admin)
2. WiFi trường/quán thường chặn thiết bị thấy nhau: dùng hotspot điện thoại.
3. Khác mạng: `npx cloudflared tunnel --url http://localhost:5173`

## Cấu trúc
- `server/index.js`: API CRUD (Express), lưu vào `server/data.json`
- `src/App.jsx`: giao diện React gọi `/api/students`
- `vite.config.js`: `host: true` + proxy `/api` về backend
