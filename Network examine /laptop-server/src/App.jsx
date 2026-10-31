import { useEffect, useMemo, useRef, useState } from 'react';

const EMPTY = { mssv: '', hoTen: '', lop: '', email: '' };

// Gọi API bằng đường dẫn tương đối -> chạy được cả trên laptop lẫn điện thoại
async function api(url, options) {
  const res = await fetch(url, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  });
  if (res.status === 204) return null;
  const data = await res.json();
  if (!res.ok) throw new Error(data.error || 'Có lỗi xảy ra');
  return data;
}

const initials = (name) => name.trim().split(/\s+/).slice(-2).map((w) => w[0]).join('').toUpperCase();

export default function App() {
  const [students, setStudents] = useState([]);
  const [query, setQuery] = useState('');
  const [form, setForm] = useState(EMPTY);
  const [editingId, setEditingId] = useState(null);
  const [error, setError] = useState('');
  const [formError, setFormError] = useState('');
  const [loading, setLoading] = useState(true);
  const dialog = useRef(null);

  const load = async () => {
    try {
      setStudents(await api('/api/students'));
      setError('');
    } catch (e) {
      setError('Không kết nối được server. Kiểm tra backend đã chạy chưa.');
    } finally {
      setLoading(false);
    }
  };
  useEffect(() => { load(); }, []);

  const shown = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return students;
    return students.filter((s) => [s.mssv, s.hoTen, s.lop, s.email].some((v) => v.toLowerCase().includes(q)));
  }, [students, query]);

  const openForm = (student) => {
    setEditingId(student ? student.id : null);
    setForm(student ? { mssv: student.mssv, hoTen: student.hoTen, lop: student.lop, email: student.email } : EMPTY);
    setFormError('');
    dialog.current.showModal();
  };

  const submit = async (e) => {
    e.preventDefault();
    try {
      if (editingId) await api(`/api/students/${editingId}`, { method: 'PUT', body: JSON.stringify(form) });
      else await api('/api/students', { method: 'POST', body: JSON.stringify(form) });
      dialog.current.close();
      await load();
    } catch (err) {
      setFormError(err.message);
    }
  };

  const remove = async (s) => {
    if (!confirm(`Xóa sinh viên ${s.hoTen}?`)) return;
    try {
      await api(`/api/students/${s.id}`, { method: 'DELETE' });
      await load();
    } catch (err) {
      alert(err.message);
    }
  };

  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  return (
    <main className="app">
      <header className="top">
        <div>
          <h1>Sổ sinh viên</h1>
          <p className="count">{students.length} sinh viên</p>
        </div>
        <button className="btn primary" onClick={() => openForm(null)}>Thêm sinh viên</button>
      </header>

      <input
        className="search"
        type="search"
        placeholder="Tìm theo tên, MSSV, lớp..."
        value={query}
        onChange={(e) => setQuery(e.target.value)}
      />

      {error && <p className="alert">{error}</p>}
      {loading && <p className="empty">Đang tải...</p>}
      {!loading && shown.length === 0 && !error && (
        <p className="empty">{query ? 'Không có kết quả phù hợp.' : 'Chưa có sinh viên nào. Bấm “Thêm sinh viên” để bắt đầu.'}</p>
      )}

      <ul className="list">
        {shown.map((s) => (
          <li key={s.id} className="row">
            <span className="avatar" aria-hidden="true">{initials(s.hoTen)}</span>
            <div className="info">
              <strong>{s.hoTen}</strong>
              <span>{s.mssv}{s.lop && `, lớp ${s.lop}`}</span>
              {s.email && <span className="mail">{s.email}</span>}
            </div>
            <div className="actions">
              <button className="btn" onClick={() => openForm(s)}>Sửa</button>
              <button className="btn danger" onClick={() => remove(s)}>Xóa</button>
            </div>
          </li>
        ))}
      </ul>

      <dialog ref={dialog} className="sheet" onClick={(e) => e.target === dialog.current && dialog.current.close()}>
        <form onSubmit={submit}>
          <h2>{editingId ? 'Sửa sinh viên' : 'Thêm sinh viên'}</h2>
          <label>MSSV<input value={form.mssv} onChange={set('mssv')} required /></label>
          <label>Họ và tên<input value={form.hoTen} onChange={set('hoTen')} required /></label>
          <label>Lớp<input value={form.lop} onChange={set('lop')} /></label>
          <label>Email<input type="email" value={form.email} onChange={set('email')} /></label>
          {formError && <p className="alert">{formError}</p>}
          <div className="form-actions">
            <button type="button" className="btn" onClick={() => dialog.current.close()}>Hủy</button>
            <button type="submit" className="btn primary">{editingId ? 'Lưu thay đổi' : 'Thêm sinh viên'}</button>
          </div>
        </form>
      </dialog>
    </main>
  );
}
