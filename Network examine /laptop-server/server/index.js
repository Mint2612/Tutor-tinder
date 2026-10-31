import express from 'express';
import fs from 'fs';
import os from 'os';
import path from 'path';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const DB_FILE = path.join(__dirname, 'data.json');
const PORT = 5000;

// "Database" đơn giản: lưu vào file JSON (đủ cho demo, khỏi cài MySQL/Mongo)
const seed = [
  { id: 1, mssv: 'SV001', hoTen: 'Nguyễn Văn An', lop: 'CNTT01', email: 'an@example.com' },
  { id: 2, mssv: 'SV002', hoTen: 'Trần Thị Bình', lop: 'CNTT02', email: 'binh@example.com' },
  { id: 3, mssv: 'SV003', hoTen: 'Lê Hoàng Cường', lop: 'CNTT01', email: 'cuong@example.com' },
];
const save = (rows) => fs.writeFileSync(DB_FILE, JSON.stringify(rows, null, 2));
const load = () => {
  try { return JSON.parse(fs.readFileSync(DB_FILE, 'utf8')); }
  catch { save(seed); return seed; }
};

const app = express();
app.use(express.json());

const clean = (b) => ({
  mssv: String(b.mssv ?? '').trim(),
  hoTen: String(b.hoTen ?? '').trim(),
  lop: String(b.lop ?? '').trim(),
  email: String(b.email ?? '').trim(),
});
const invalid = (s) => (!s.mssv || !s.hoTen ? 'MSSV và họ tên không được để trống' : null);

// READ
app.get('/api/students', (_req, res) => res.json(load()));

// CREATE
app.post('/api/students', (req, res) => {
  const s = clean(req.body);
  const err = invalid(s);
  if (err) return res.status(400).json({ error: err });
  const rows = load();
  if (rows.some((r) => r.mssv === s.mssv)) return res.status(409).json({ error: 'MSSV đã tồn tại' });
  const row = { id: Date.now(), ...s };
  save([...rows, row]);
  res.status(201).json(row);
});

// UPDATE
app.put('/api/students/:id', (req, res) => {
  const id = Number(req.params.id);
  const s = clean(req.body);
  const err = invalid(s);
  if (err) return res.status(400).json({ error: err });
  const rows = load();
  const i = rows.findIndex((r) => r.id === id);
  if (i < 0) return res.status(404).json({ error: 'Không tìm thấy sinh viên' });
  if (rows.some((r) => r.mssv === s.mssv && r.id !== id)) return res.status(409).json({ error: 'MSSV đã tồn tại' });
  rows[i] = { id, ...s };
  save(rows);
  res.json(rows[i]);
});

// DELETE
app.delete('/api/students/:id', (req, res) => {
  const id = Number(req.params.id);
  const rows = load();
  if (!rows.some((r) => r.id === id)) return res.status(404).json({ error: 'Không tìm thấy sinh viên' });
  save(rows.filter((r) => r.id !== id));
  res.status(204).end();
});

// 0.0.0.0 = nhận kết nối từ mọi máy trong mạng, không chỉ localhost
app.listen(PORT, '0.0.0.0', () => {
  console.log(`API chạy ở cổng ${PORT}`);
  for (const list of Object.values(os.networkInterfaces()))
    for (const n of list ?? [])
      if (n.family === 'IPv4' && !n.internal)
        console.log(`\n>>> Mở trên điện thoại: http://${n.address}:5173 <<<\n`);
});
