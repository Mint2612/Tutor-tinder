import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    host: true,          // cho máy khác trong mạng truy cập (0.0.0.0)
    port: 5173,
    allowedHosts: true,  // cho phép truy cập qua tunnel (ngrok, cloudflared...)
    proxy: {
      // Frontend gọi /api/... -> Vite chuyển tiếp về backend ở localhost:5000
      // => điện thoại chỉ cần biết 1 địa chỉ, không lo CORS hay IP backend
      '/api': 'http://localhost:5000',
    },
  },
});
