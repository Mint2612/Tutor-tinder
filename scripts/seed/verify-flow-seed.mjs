// Kiểm tra seed bằng API thật: node scripts/seed/verify-flow-seed.mjs [baseUrl]
// Chỉ ĐỌC (không thay đổi dữ liệu), nên có thể chạy lại không cần nạp lại seed.
const base = process.argv[2] || "http://localhost:3000";
const password = "TutorMatch@Test2026";
let failed = 0;
const check = (ok, label) => { console.log(`${ok ? "PASS" : "FAIL"}  ${label}`); if (!ok) failed++; };

async function session(email) {
  const jar = new Map();
  const send = async (path, opts = {}) => {
    const res = await fetch(base + "/api" + path, { ...opts, headers: { "Content-Type": "application/json",
      Cookie: [...jar].map(([k, v]) => `${k}=${v}`).join("; "), ...(opts.headers || {}) } });
    for (const c of res.headers.getSetCookie()) { const [kv] = c.split(";"); const i = kv.indexOf("="); jar.set(kv.slice(0, i), kv.slice(i + 1)); }
    const text = await res.text();
    return { status: res.status, body: text ? JSON.parse(text) : null };
  };
  const csrf = await send("/auth/csrf");
  const login = await send("/auth/login", { method: "POST", headers: { "X-XSRF-TOKEN": csrf.body.token }, body: JSON.stringify({ email, password }) });
  if (login.status !== 200) throw new Error(`Đăng nhập ${email}: ${login.status} ${JSON.stringify(login.body)}`);
  return { get: (p) => send(p), account: login.body };
}
const e = (name) => `flow-${name}@example.test`;

// Bước 1: phụ huynh chưa có hồ sơ
let s = await session(e("phuhuynh-moi"));
check((await s.get("/students")).body.length === 0, "B1  Phụ huynh mới chưa có hồ sơ học sinh (để tự tạo hồ sơ + khảo sát)");
// Phụ huynh có hồ sơ + khảo sát đủ 6 câu
s = await session(e("phuhuynh-1"));
const st1 = (await s.get("/students")).body[0];
check(st1 && Object.keys(st1.survey).length === 6, "B1  Phụ huynh 1 có hồ sơ học sinh + khảo sát 6 câu");

// Bước 2: gia sư chưa có hồ sơ / bản nháp / chờ duyệt
s = await session(e("giasu-moi"));
check((await s.get("/tutor/profile")).status === 404, "B2  Gia sư mới chưa có hồ sơ (để tự tạo)");
s = await session(e("giasu-nhap"));
let p = (await s.get("/tutor/profile")).body;
check(p.workflow_state === "DRAFT" && (await s.get("/tutor/documents")).body.length === 1, "B2  Gia sư nháp: có giấy tờ, chưa gửi duyệt");
s = await session(e("giasu-choduyet1"));
p = (await s.get("/tutor/profile")).body;
check(p.workflow_state === "PENDING_REVIEW", "B2  Gia sư chờ duyệt: đã gửi yêu cầu xác minh");

// Bước 3: admin thấy hàng chờ
s = await session(e("admin"));
const queue = (await s.get("/admin/tutors")).body;
const pending = queue.filter((t) => t.workflow_state === "PENDING_REVIEW" && /^Gia sư Chờ duyệt/.test(t.display_name));
check(pending.length === 2, "B3  Admin thấy 2 gia sư chờ duyệt trong hàng đợi");

// Bước 4: phụ huynh 1 thấy gia sư đã duyệt để bấm OK (gia sư chờ duyệt thì chưa hiện)
s = await session(e("phuhuynh-1"));
let feed = (await s.get(`/students/${st1.id}/matching`)).body;
const names = feed.cards.map((c) => c.tutor.displayName).sort();
check(names.join("|") === "Cô Hoa Test|Cô Lan Test|Thầy Bình Test", `B4  Phụ huynh 1 thấy đúng 3 thẻ gia sư đã duyệt (${names.join(", ")})`);

// Bước 5: gia sư thấy nhãn "Quan tâm bạn"
s = await session(e("giasu-quantam"));
feed = (await s.get("/tutor/matching")).body;
check(feed.cards.length === 1 && feed.cards[0].tag === "INTERESTED_IN_YOU", "B5  Gia sư thấy thẻ có nhãn INTERESTED_IN_YOU (chưa Match)");
check(!JSON.stringify(feed.cards[0]).includes("flow-phuhuynh-2"), "B5  Thẻ học sinh ẩn danh (không lộ liên hệ phụ huynh)");

// Bước 6: Match đã tạo, chưa ai xác nhận → chưa lộ liên hệ
s = await session(e("phuhuynh-3"));
let sid = (await s.get("/students")).body[0].id;
feed = (await s.get(`/students/${sid}/matching`)).body;
check(feed.matches.length === 1, "B6  Phụ huynh 3 có 1 Match");
let d = (await s.get(`/matching/pairs/${sid}/${feed.matches[0].tutorId}`)).body;
check(!d.connected && !d.contact && !d.myDecision, "B6  Match chưa xác nhận: chưa có liên hệ, chờ cả hai quyết định");
check(((await s.get("/matching/notifications")).body).some((n) => n.eventKey === "MATCH"), "B6  Có thông báo Match chưa đọc");

// Bước 7a: phụ huynh đã xác nhận, chờ gia sư
s = await session(e("phuhuynh-4"));
sid = (await s.get("/students")).body[0].id;
feed = (await s.get(`/students/${sid}/matching`)).body;
d = (await s.get(`/matching/pairs/${sid}/${feed.matches[0].tutorId}`)).body;
check(d.myDecision === "CONFIRM" && !d.connected && !d.contact, "B7a Phụ huynh đã xác nhận, đang chờ gia sư, liên hệ vẫn ẩn");
s = await session(e("giasu-choxacnhan"));
d = (await s.get(`/matching/pairs/${sid}/${feed.matches[0].tutorId}`)).body;
check(d.myDecision == null && !d.connected && !d.contact, "B7a Gia sư thấy cần xác nhận kết nối");

// Bước 7b: đã kết nối, có liên hệ + đề xuất học thử đang chờ gia sư
s = await session(e("phuhuynh-5"));
sid = (await s.get("/students")).body[0].id;
feed = (await s.get(`/students/${sid}/matching`)).body;
d = (await s.get(`/matching/pairs/${sid}/${feed.matches[0].tutorId}`)).body;
check(d.connected && d.contact?.email === e("giasu-hocthu"), "B7b Phụ huynh thấy liên hệ gia sư sau khi hai bên xác nhận");
s = await session(e("giasu-hocthu"));
d = (await s.get(`/matching/pairs/${sid}/${feed.matches[0].tutorId}`)).body;
check(d.connected && d.contact?.email === e("phuhuynh-5"), "B7b Gia sư thấy liên hệ phụ huynh sau khi hai bên xác nhận");
const trials = (await s.get("/trials")).body;
check(trials.length === 1 && trials[0].status === "PENDING" && trials[0].proposerRole === "PARENT" && trials[0].slots.length === 3,
  "B7b Có đề xuất học thử (3 khung giờ) chờ gia sư chốt");

// Bước 7c: đã kết nối, chưa có lịch học thử
s = await session(e("phuhuynh-6"));
sid = (await s.get("/students")).body[0].id;
feed = (await s.get(`/students/${sid}/matching`)).body;
d = (await s.get(`/matching/pairs/${sid}/${feed.matches[0].tutorId}`)).body;
check(d.connected && (await s.get("/trials")).body.length === 0, "B7c Đã kết nối, chưa có lịch học thử (để tự đề xuất)");

console.log(failed ? `\n${failed} kiểm tra thất bại` : "\nTất cả kiểm tra đạt");
process.exit(failed ? 1 : 0);
