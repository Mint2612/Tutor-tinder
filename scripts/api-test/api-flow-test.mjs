#!/usr/bin/env node
// Kiểm thử API end-to-end cho TutorMatch (Node 18+, không cần cài thêm gì).
//
//   node scripts/api-test/api-flow-test.mjs            # toàn bộ luồng 7 bước + kiểm tra bảo mật
//   node scripts/api-test/api-flow-test.mjs --smoke    # chỉ kiểm tra nhanh, không tạo dữ liệu
//   node scripts/api-test/api-flow-test.mjs --mail     # gia sư đăng ký + kích hoạt qua email (cần Mailpit)
//
// Gia sư: mặc định KHÔNG gửi email nào. Đăng ký gia sư luôn gửi email kích hoạt; stack local có thể đang dùng
// SMTP thật (ví dụ Gmail trong .env) nên email tới địa chỉ giả sẽ bị trả lại. Vì vậy mặc định script tạo tài khoản
// gia sư trực tiếp trong DB local (docker compose exec db; mật khẩu TutorDemo!2026# sao từ seed.tutor001).
// Dùng --mail chỉ khi backend gửi mail vào Mailpit (stack e2e: BASE_URL=http://127.0.0.1:33000
// MAILPIT_URL=http://127.0.0.1:38025 ADMIN_EMAIL=e2e-admin@example.test ADMIN_PASSWORD=e2e-admin-password-12345).
// Phụ huynh luôn đăng ký qua API (không gửi email).
//
// Biến môi trường (có mặc định cho môi trường local):
//   BASE_URL        http://localhost:3000        (đi qua nginx của frontend, /api được proxy)
//   MAILPIT_URL     http://localhost:8025        (đọc email kích hoạt của gia sư)
//   ADMIN_EMAIL     demo.flow.admin@tutormatch.local
//   ADMIN_PASSWORD  TutorDemo!2026#
//
// Luồng đầy đủ tạo 1 phụ huynh + 1 gia sư mới (email apitest-*@example.test, học trực tiếp tại Quảng Ninh
// để không lẫn vào dữ liệu khác), đi qua: tạo hồ sơ + khảo sát → gia sư gửi xác minh → admin duyệt → phụ huynh OK
// → gia sư thấy nhãn "Quan tâm bạn" và OK → Match → xác nhận hai chiều → liên hệ → đề xuất/chốt giờ học thử →
// trang giám sát. Dữ liệu tạo ra là dữ liệu giả, không bị xóa sau khi chạy.
// Giới hạn của server: 8 lượt đăng ký/IP/15 phút và 20 lượt đăng nhập/IP/15 phút, nên chạy tối đa vài lần liên tiếp.
// Mã thoát: 0 = tất cả đạt, 1 = có kiểm tra thất bại.

import { execFileSync } from "node:child_process";
import path from "node:path";
import { fileURLToPath } from "node:url";

const BASE = (process.env.BASE_URL || "http://localhost:3000").replace(/\/$/, "");
const MAILPIT = (process.env.MAILPIT_URL || "http://localhost:8025").replace(/\/$/, "");
const ADMIN = {
  email: process.env.ADMIN_EMAIL || "demo.flow.admin@tutormatch.local",
  password: process.env.ADMIN_PASSWORD || "TutorDemo!2026#",
};
const SMOKE = process.argv.includes("--smoke");
const MAIL = process.argv.includes("--mail");
const REPO = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../..");
const TUTOR_DB_PASSWORD = "TutorDemo!2026#";
const PASSWORD = "ApiTest!2026-pass";
const SURVEY = { goal: "foundation", learning: "practice", difficulty: "understanding", pace: "step_by_step", support: "feedback", lesson: "examples" };

// ---------------------------------------------------------------- khung kiểm thử
let passed = 0, failed = 0, group = "";
const failures = [];
const section = (name) => { group = name; console.log(`\n== ${name}`); };
function check(ok, label, detail) {
  if (ok) { passed++; console.log(`  PASS  ${label}`); }
  else { failed++; failures.push(`${group}: ${label}`); console.log(`  FAIL  ${label}${detail !== undefined ? `\n        -> ${typeof detail === "string" ? detail : JSON.stringify(detail)}` : ""}`); }
  return ok;
}
const is = (res, status, label) => check(res.status === status, `${label} -> HTTP ${status}`, `nhận HTTP ${res.status}: ${JSON.stringify(res.body)?.slice(0, 200)}`);
const must = (value, label) => { if (!value) { console.log(`  STOP  ${label}`); finish(true); } return value; };

// ---------------------------------------------------------------- client có cookie + CSRF
class Client {
  constructor(name) { this.name = name; this.jar = new Map(); }
  async raw(path, { method = "GET", body, form, csrf = true, headers = {} } = {}) {
    const h = { ...headers };
    if (method !== "GET" && csrf) h["X-XSRF-TOKEN"] = (await this.raw("/auth/csrf")).body.token;
    let payload;
    if (form) payload = form; else if (body !== undefined) { payload = JSON.stringify(body); h["Content-Type"] = "application/json"; }
    const cookie = [...this.jar].map(([k, v]) => `${k}=${v}`).join("; ");
    if (cookie) h.Cookie = cookie;
    const res = await fetch(`${BASE}/api${path}`, { method, headers: h, body: payload });
    for (const c of res.headers.getSetCookie()) { const [kv] = c.split(";"); const i = kv.indexOf("="); this.jar.set(kv.slice(0, i), kv.slice(i + 1)); }
    const text = await res.text();
    let parsed = null; try { parsed = text ? JSON.parse(text) : null; } catch { parsed = text; }
    return { status: res.status, body: parsed, headers: res.headers, bytes: text.length };
  }
  get(path) { return this.raw(path); }
  post(path, body, o) { return this.raw(path, { method: "POST", body, ...o }); }
  put(path, body, o) { return this.raw(path, { method: "PUT", body, ...o }); }
  async login(email, password) {
    const res = await this.post("/auth/login", { email, password });
    return res;
  }
}

async function mailpitLink(email, timeoutMs = 20000) {
  const until = Date.now() + timeoutMs;
  while (Date.now() < until) {
    try {
      const list = await (await fetch(`${MAILPIT}/api/v1/search?query=${encodeURIComponent("to:" + email)}`)).json();
      const id = list.messages?.[0]?.ID;
      if (id) {
        const msg = await (await fetch(`${MAILPIT}/api/v1/message/${id}`)).json();
        const m = (msg.Text || "").match(/#\/verify\?([^\s]+)/);
        if (m) return Object.fromEntries(new URLSearchParams(m[1]));
      }
    } catch { /* Mailpit chưa sẵn sàng, thử lại */ }
    await new Promise((r) => setTimeout(r, 700));
  }
  return null;
}

function finish(early = false) {
  console.log(`\n${"-".repeat(60)}\n${passed} đạt, ${failed} thất bại${early ? " (dừng sớm)" : ""}`);
  if (failures.length) { console.log("Thất bại:"); failures.forEach((f) => console.log(`  - ${f}`)); }
  process.exit(failed ? 1 : 0);
}

// ================================================================ 0. Sức khỏe + bảo mật cơ bản (không cần tài khoản)
section("0. Sức khỏe và bảo vệ cơ bản");
const anon = new Client("anon");
const health = await anon.get("/health");
is(health, 200, "GET /health");
check(health.body?.status === "ok", "health trả status ok", health.body);
is(await anon.get("/students"), 401, "chưa đăng nhập: GET /students bị từ chối");
is(await anon.get("/admin/monitor"), 401, "chưa đăng nhập: GET /admin/monitor bị từ chối");
const noCsrf = await anon.raw("/auth/login", { method: "POST", body: { email: "a@b.test", password: "x" }, csrf: false });
is(noCsrf, 403, "POST thiếu CSRF bị từ chối");
const bad = await anon.post("/auth/login", { email: ADMIN.email, password: "sai-mat-khau-123456" });
check(bad.status === 401 || bad.status === 400, "đăng nhập sai mật khẩu bị từ chối", bad.status);
check(!JSON.stringify(bad.body).includes(ADMIN.password), "thông báo lỗi không lộ mật khẩu");

section("0b. Quản trị viên và trang giám sát");
const admin = new Client("admin");
const adminLogin = await admin.login(ADMIN.email, ADMIN.password);
is(adminLogin, 200, `đăng nhập admin (${ADMIN.email})`);
must(adminLogin.status === 200, "Không đăng nhập được admin: kiểm tra ADMIN_EMAIL/ADMIN_PASSWORD");
check(adminLogin.body?.role === "ADMIN", "tài khoản có vai trò ADMIN");
const mon = await admin.get("/admin/monitor");
is(mon, 200, "GET /admin/monitor");
check(mon.body?.activity?.length === 7, "monitor có 7 ngày hoạt động", mon.body?.activity?.length);
check(["accounts", "tutors", "matching", "trials", "server"].every((k) => mon.body?.[k]), "monitor có đủ nhóm số liệu");
check(!/@/.test(JSON.stringify(mon.body)), "monitor không chứa email/liên hệ");
check(mon.body?.server?.database === true, "cơ sở dữ liệu phản hồi");
is(await admin.get("/admin/tutors"), 200, "GET /admin/tutors (hàng chờ duyệt)");

if (SMOKE) { console.log("\n(--smoke) bỏ qua phần tạo dữ liệu."); finish(); }

// ================================================================ 1. Đăng ký
const stamp = `${Date.now().toString(36)}${Math.random().toString(36).slice(2, 6)}`;
const parentEmail = `apitest-parent-${stamp}@example.test`;
const tutorEmail = `apitest-tutor-${stamp}@example.test`;
const TUTOR_NAME = `Gia sư API ${stamp}`;
const parent = new Client("parent"), tutor = new Client("tutor");

section("1. Đăng ký và kích hoạt tài khoản");
is(await anon.post("/auth/register", { email: `x-${stamp}@example.test`, password: PASSWORD, name: "Bad", phone: "12345", role: "PARENT" }), 400, "số điện thoại sai định dạng bị từ chối");
is(await anon.post("/auth/register", { email: `y-${stamp}@example.test`, password: "ngan", name: "Bad", phone: "0901234567", role: "PARENT" }), 400, "mật khẩu < 12 ký tự bị từ chối");
is(await anon.post("/auth/register", { email: `z-${stamp}@example.test`, password: PASSWORD, name: "Bad", phone: "0901234567", role: "ADMIN" }), 400, "không đăng ký được vai trò ADMIN");
is(await anon.post("/auth/register", { email: parentEmail, password: PASSWORD, name: `Phụ huynh API ${stamp}`, phone: "0901000111", role: "PARENT" }), 200, "đăng ký phụ huynh");
let tutorPassword = PASSWORD;
if (MAIL) {
  is(await anon.post("/auth/register", { email: tutorEmail, password: PASSWORD, name: TUTOR_NAME, phone: "0901000222", role: "TUTOR" }), 200, "đăng ký gia sư (gửi email kích hoạt)");
  const early = await new Client("early").login(tutorEmail, PASSWORD);
  check(early.status >= 400, "gia sư chưa kích hoạt email thì chưa đăng nhập được", early.status);
  const link = await mailpitLink(tutorEmail);
  must(link, `không thấy email kích hoạt của ${tutorEmail} trong Mailpit (${MAILPIT}); backend có đang gửi vào Mailpit không?`);
  check(!!link.challengeId && !!link.code, "email kích hoạt có challengeId và code");
  is(await anon.post("/auth/verify", { challengeId: link.challengeId, code: link.code }), 200, "kích hoạt email gia sư");
  is(await anon.post("/auth/verify", { challengeId: link.challengeId, code: link.code }), 400, "mã kích hoạt chỉ dùng được một lần");
} else {
  // Không gửi email: tạo tài khoản đã kích hoạt trực tiếp trong DB local, cùng mật khẩu với seed.tutor001.
  const sql = `INSERT INTO users (id,email,full_name,role,password_hash,phone,email_verified,phone_verified) SELECT UUID(),'${tutorEmail}','${TUTOR_NAME}','TUTOR',password_hash,'0901000222',TRUE,FALSE FROM users WHERE email='seed.tutor001@tutormatch.local'; SELECT ROW_COUNT();`;
  let out = "";
  try {
    out = execFileSync("docker", ["compose", "exec", "-T", "db", "sh", "-c", 'mariadb -N -u"$MARIADB_USER" -p"$MARIADB_PASSWORD" "$MARIADB_DATABASE" -e "' + sql.replace(/"/g, '\\"') + '"'], { cwd: REPO, encoding: "utf8" });
  } catch (e) { out = String(e.stderr || e.message); }
  check(out.trim().endsWith("1"), "tạo tài khoản gia sư đã kích hoạt trong DB local (không gửi email)", out.slice(0, 200));
  must(out.trim().endsWith("1"), "cần docker compose + seed.tutor001; hoặc chạy với --mail trên stack có Mailpit");
  tutorPassword = TUTOR_DB_PASSWORD;
}
const pl = await parent.login(parentEmail, PASSWORD);
is(pl, 200, "phụ huynh đăng nhập (không cần xác thực email)");
const tl = await tutor.login(tutorEmail, tutorPassword);
is(tl, 200, "gia sư đăng nhập sau khi kích hoạt");
is(await parent.get("/admin/tutors"), 403, "phụ huynh không vào được API admin");
is(await parent.get("/admin/monitor"), 403, "phụ huynh không vào được monitor");
is(await tutor.get("/admin/monitor"), 403, "gia sư không vào được monitor");

// ================================================================ 2. Bước 1: phụ huynh tạo hồ sơ + khảo sát
section("2. Phụ huynh tạo hồ sơ học sinh và khảo sát");
const studentBody = {
  nickname: `Bé API ${stamp}`, grade: 6, subjects: ["Toán"], province: "Quảng Ninh", mode: "IN_PERSON", budget: 250000,
  availability: ["1_19:00-20:30", "3_19:00-20:30"], needs: "Củng cố nền tảng Toán, cần giải thích từng bước.", survey: SURVEY, consent: true,
};
is(await parent.post("/students", { ...studentBody, consent: false }), 400, "thiếu đồng ý của phụ huynh bị từ chối");
is(await parent.post("/students", { ...studentBody, needs: "Gọi phụ huynh theo số 0903 123 456" }), 400, "mô tả chứa số điện thoại bị từ chối");
is(await parent.post("/students", { ...studentBody, province: "Không tồn tại" }), 400, "tỉnh không có trong danh sách bị từ chối");
const created = await parent.post("/students", studentBody);
is(created, 200, "tạo hồ sơ học sinh");
const studentId = must(created.body?.id, "không có id học sinh");
check(Object.keys(created.body.survey || {}).length === 6, "khảo sát lưu đủ 6 câu");
check((await parent.get("/students")).body?.some((s) => s.id === studentId), "hồ sơ có trong danh sách của phụ huynh");
is(await tutor.get(`/students/${studentId}`), 403, "gia sư không đọc được hồ sơ học sinh gốc");

// ================================================================ 3. Bước 2: gia sư tạo hồ sơ + gửi xác minh
section("3. Gia sư tạo hồ sơ và yêu cầu xác minh");
const tutorBody = {
  fullName: "Nguyễn Văn Kiểm Thử API", photoUrl: "", subjects: ["Toán"], grades: [6], modes: ["IN_PERSON"], province: "Quảng Ninh",
  hourlyRate: 150000, methodology: "Kiên nhẫn, luyện từng dạng bài.", style: "Kiên nhẫn, hướng dẫn từng bước",
  background: "Cử nhân Sư phạm Toán, có kinh nghiệm dạy kèm.", experience: 3, availability: ["1_19:00-20:30", "3_19:00-20:30", "5_19:00-20:30"], achievements: "",
};
is(await tutor.get("/tutor/profile"), 404, "gia sư mới chưa có hồ sơ");
is(await tutor.post("/tutor/submit", {}), 404, "chưa có hồ sơ thì chưa gửi duyệt được");
is(await tutor.put("/tutor/profile", { ...tutorBody, hourlyRate: 0 }), 400, "học phí không hợp lệ bị từ chối");
const saved = await tutor.put("/tutor/profile", tutorBody);
is(saved, 200, "lưu hồ sơ gia sư");
const tutorId = must(saved.body?.id, "không có id gia sư");
check(saved.body?.workflow_state === "DRAFT", "hồ sơ mới ở trạng thái DRAFT", saved.body?.workflow_state);
check(saved.body?.display_name === TUTOR_NAME, "tên hiển thị lấy từ tên đăng ký", saved.body?.display_name);
is(await tutor.post("/tutor/submit", {}), 400, "chưa có giấy tờ thì chưa gửi duyệt được");
const fd = (bytes, name, type) => { const f = new FormData(); f.append("file", new Blob([bytes], { type }), name); return f; };
is(await tutor.raw("/tutor/documents", { method: "POST", form: fd("không phải pdf", "gia-gia.pdf", "application/pdf") }), 400, "tệp giả mạo PDF bị từ chối (kiểm tra magic byte)");
const pdf = "%PDF-1.4\n% Giay to gia dung cho kiem thu API\n%%EOF\n";
const doc = await tutor.raw("/tutor/documents", { method: "POST", form: fd(pdf, "giay-to-gia-api.pdf", "application/pdf") });
is(doc, 200, "tải giấy tờ danh tính (PDF)");
const docId = doc.body?.id;
const submit = await tutor.post("/tutor/submit", {});
is(submit, 200, "gửi hồ sơ để duyệt");
check(submit.body?.workflow_state === "PENDING_REVIEW", "hồ sơ chuyển sang PENDING_REVIEW", submit.body?.workflow_state);
check(!(await parent.get(`/students/${studentId}/matching`)).body?.cards?.some((c) => c.tutorId === tutorId), "gia sư chờ duyệt chưa hiện trong danh sách của phụ huynh");

// ================================================================ 4. Bước 3: admin duyệt
section("4. Admin xác minh gia sư");
const queue = (await admin.get("/admin/tutors")).body || [];
check(queue.some((t) => t.id === tutorId && t.workflow_state === "PENDING_REVIEW"), "gia sư có trong hàng chờ của admin");
const review = await admin.get(`/admin/tutors/${tutorId}`);
check(review.body?.full_name === tutorBody.fullName, "admin thấy họ tên khai báo để đối chiếu giấy tờ", review.body?.full_name);
is(await tutor.get(`/admin/tutors/${tutorId}`), 403, "gia sư không vào được API admin");
is(await admin.post(`/admin/tutors/${tutorId}/decision`, { approve: true }), 400, "chưa mở giấy tờ thì không duyệt được");
is(await admin.post(`/admin/tutors/${tutorId}/decision`, { approve: false }), 400, "từ chối phải kèm lý do");
const download = await admin.get(`/admin/documents/${docId}`);
is(download, 200, "admin mở giấy tờ");
check(download.headers.get("cache-control")?.includes("no-store"), "giấy tờ không được lưu cache");
is(await admin.post(`/admin/tutors/${tutorId}/decision`, { approve: true }), 200, "admin phê duyệt");
const approved = await tutor.get("/tutor/profile");
check(approved.body?.workflow_state === "APPROVED" && approved.body?.verification_status === "APPROVED", "gia sư ở trạng thái APPROVED");
check((await parent.get(`/tutors/${tutorId}`)).status === 403 || (await parent.get(`/tutors/${tutorId}`)).status === 404, "chi tiết đầy đủ của gia sư chưa mở trước khi Match");

// ================================================================ 5. Bước 4: phụ huynh bấm OK
section("5. Phụ huynh bấm OK trên thẻ gia sư");
const feed1 = await parent.get(`/students/${studentId}/matching`);
is(feed1, 200, "GET feed của học sinh");
const card = feed1.body?.cards?.find((c) => c.tutorId === tutorId);
must(card, "thẻ gia sư vừa duyệt không có trong danh sách của phụ huynh");
check(card.tutor?.displayName === TUTOR_NAME && card.tag === "SUGGESTED", "thẻ gia sư hiển thị đúng, nhãn SUGGESTED");
check(!JSON.stringify(card).includes(tutorEmail) && !("fullName" in (card.tutor || {})), "thẻ gia sư không lộ email hay họ tên giấy tờ");
const ai = await parent.get(`/students/${studentId}/matching/ai`);
is(ai, 200, "GET gợi ý AI (có dự phòng theo luật)");
check(Array.isArray(ai.body) && ai.body.length <= 10, "gợi ý AI tối đa 10 hồ sơ", ai.body?.length);
const ok1 = await parent.post("/matching/actions", { studentId, tutorId, interested: true });
is(ok1, 200, "phụ huynh OK");
check(ok1.body?.status === "OPEN" && ok1.body?.matched === false, "chưa Match (chờ gia sư)", ok1.body);
const ok1b = await parent.post("/matching/actions", { studentId, tutorId, interested: true });
check(ok1b.status === 200 && ok1b.body?.matched === false, "OK lặp lại được xử lý idempotent", ok1b.body);
is(await parent.post("/matching/actions", { studentId, tutorId, interested: false }), 409, "không đổi được quyết định đã lưu");
is(await tutor.post("/matching/actions", { studentId: "00000000-0000-4000-8000-000000000000", tutorId, interested: true }), 404, "gia sư OK học sinh không tồn tại bị từ chối");
check((await parent.get(`/students/${studentId}/matching`)).body?.savedCards?.some((c) => c.tutorId === tutorId), "lựa chọn nằm trong 'Đã OK & Match'");

// ================================================================ 6. Bước 5: gia sư thấy nhãn rồi OK -> Match
section("6. Gia sư thấy “Quan tâm bạn” và bấm OK → Match");
const tfeed = await tutor.get("/tutor/matching");
is(tfeed, 200, "GET feed của gia sư");
const tcard = tfeed.body?.cards?.find((c) => c.studentId === studentId);
must(tcard, "học sinh không có trong danh sách của gia sư");
check(tcard.tag === "INTERESTED_IN_YOU", "thẻ có nhãn INTERESTED_IN_YOU", tcard.tag);
check(tfeed.body.cards[0].tag === "INTERESTED_IN_YOU", "thẻ có nhãn đứng đầu danh sách");
const anonymous = JSON.stringify(tcard);
check(![parentEmail, studentBody.nickname, "Phụ huynh API", "0901000111"].some((x) => anonymous.includes(x)), "thẻ học sinh ẩn danh (không email/tên/số điện thoại)");
const ok2 = await tutor.post("/matching/actions", { studentId, tutorId, interested: true });
is(ok2, 200, "gia sư OK");
check(ok2.body?.matched === true && ok2.body?.status === "MATCHED", "tạo Match", ok2.body);
const pn = await parent.get("/matching/notifications");
check(pn.body?.some((n) => n.eventKey === "MATCH" && n.studentId === studentId), "phụ huynh có thông báo Match");
check((await tutor.get("/matching/notifications")).body?.some((n) => n.eventKey === "MATCH"), "gia sư có thông báo Match");
const matchNote = pn.body?.find((n) => n.eventKey === "MATCH" && n.studentId === studentId);
if (matchNote) is(await parent.post(`/matching/notifications/${matchNote.id}/read`), 204, "đánh dấu đã đọc thông báo");

// ================================================================ 7. Bước 6-7: chi tiết, xác nhận hai chiều, liên hệ
section("7. Chi tiết, xác nhận hai chiều và liên hệ");
const detailP0 = await parent.get(`/matching/pairs/${studentId}/${tutorId}`);
is(detailP0, 200, "phụ huynh mở chi tiết Match");
check(detailP0.body?.connected === false && !detailP0.body?.contact, "trước khi xác nhận: chưa có liên hệ");
check(!JSON.stringify(detailP0.body).includes(tutorEmail), "chi tiết trước xác nhận không lộ email gia sư");
const detailT0 = await tutor.get(`/matching/pairs/${studentId}/${tutorId}`);
check(!!detailT0.body?.student && !JSON.stringify(detailT0.body).includes(parentEmail), "gia sư thấy hồ sơ học sinh ẩn danh, không có email phụ huynh");
const other = new Client("other");
is(await other.get(`/matching/pairs/${studentId}/${tutorId}`), 401, "người ngoài không xem được chi tiết Match");
const early2 = await parent.post("/trials", { studentId, tutorId, slots: [new Date(Date.now() + 3 * 864e5).toISOString()] });
check(early2.status >= 400 && early2.status < 500, "chưa kết nối thì chưa đề xuất học thử được", early2.status);
const cp = await parent.post("/matching/confirm", { studentId, tutorId, confirm: true });
is(cp, 200, "phụ huynh xác nhận kết nối");
check(cp.body?.myDecision === "CONFIRM" && cp.body?.connected === false && !cp.body?.contact, "một bên xác nhận: liên hệ vẫn ẩn", cp.body);
const cp2 = await parent.post("/matching/confirm", { studentId, tutorId, confirm: true });
check(cp2.status === 200, "xác nhận lặp lại idempotent", cp2.status);
is(await parent.post("/matching/confirm", { studentId, tutorId, confirm: false }), 409, "không đổi được quyết định xác nhận");
const ct = await tutor.post("/matching/confirm", { studentId, tutorId, confirm: true });
is(ct, 200, "gia sư xác nhận kết nối");
check(ct.body?.connected === true && ct.body?.contact?.email === parentEmail, "gia sư thấy liên hệ phụ huynh", ct.body?.contact);
const detailP1 = await parent.get(`/matching/pairs/${studentId}/${tutorId}`);
check(detailP1.body?.connected === true && detailP1.body?.contact?.email === tutorEmail, "phụ huynh thấy liên hệ gia sư", detailP1.body?.contact);
check((await parent.get(`/tutors/${tutorId}`)).status === 200, "sau Match mới xem được chi tiết đầy đủ gia sư");

// ================================================================ 8. Học thử
section("8. Lịch học thử");
const day = (n, h) => { const d = new Date(Date.now() + n * 864e5); d.setUTCHours(h, 0, 0, 0); return d.toISOString(); };
is(await parent.post("/trials", { studentId, tutorId, slots: [new Date(Date.now() - 864e5).toISOString()] }), 400, "giờ trong quá khứ bị từ chối");
is(await parent.post("/trials", { studentId, tutorId, slots: [day(4, 12), day(5, 12), day(6, 12), day(7, 12)] }), 400, "quá 3 khung giờ bị từ chối");
is(await parent.post("/trials", { studentId, tutorId, slots: [day(4, 12)], mode: "ONLINE" }), 400, "hình thức không khớp hồ sơ học sinh bị từ chối");
const trial = await parent.post("/trials", { studentId, tutorId, slots: [day(4, 12), day(5, 12), day(6, 12)] });
is(trial, 200, "phụ huynh đề xuất 3 giờ học thử");
check(trial.body?.status === "PENDING" && trial.body?.proposerRole === "PARENT" && trial.body?.mode === "IN_PERSON", "lịch PENDING, do phụ huynh đề xuất", trial.body);
is(await parent.post("/trials", { studentId, tutorId, slots: [day(4, 13)] }), 409, "mỗi cặp chỉ có một lịch đang hoạt động");
const tTrials = await tutor.get("/trials");
check(tTrials.body?.some((t) => t.id === trial.body?.id), "gia sư thấy đề xuất trong danh sách lịch");
is(await parent.put(`/trials/${trial.body.id}`, { action: "ACCEPT", version: trial.body.version, slot: trial.body.slots[0] }), 409, "người đề xuất không tự chốt lịch của mình");
is(await tutor.put(`/trials/${trial.body.id}`, { action: "ACCEPT", version: trial.body.version + 5, slot: trial.body.slots[0] }), 409, "phiên bản cũ/sai bị từ chối");
const accepted = await tutor.put(`/trials/${trial.body.id}`, { action: "ACCEPT", version: trial.body.version, slot: trial.body.slots[1] });
is(accepted, 200, "gia sư chốt một khung giờ");
check(accepted.body?.status === "CONFIRMED" && accepted.body?.chosenAt === trial.body.slots[1], "lịch CONFIRMED đúng giờ đã chọn", accepted.body);
is(await tutor.put(`/trials/${trial.body.id}`, { action: "CANCEL", version: accepted.body.version, reason: "" }), 400, "hủy lịch phải có lý do");
const cancelled = await parent.put(`/trials/${trial.body.id}`, { action: "CANCEL", version: accepted.body.version, reason: "Gia đình có lịch đột xuất." });
is(cancelled, 200, "phụ huynh hủy lịch kèm lý do");
check(cancelled.body?.status === "CANCELLED", "lịch chuyển CANCELLED", cancelled.body?.status);
is(await parent.post("/trials", { studentId, tutorId, slots: [day(5, 13)] }), 200, "sau khi hủy có thể đề xuất lịch mới");

// ================================================================ 9. Giám sát phản ánh dữ liệu mới
section("9. Trang giám sát phản ánh hoạt động");
const mon2 = await admin.get("/admin/monitor");
is(mon2, 200, "GET /admin/monitor sau khi chạy luồng");
const before = mon.body, after = mon2.body;
check(after.accounts.parents >= before.accounts.parents + 1 && after.accounts.tutors >= before.accounts.tutors + 1, "số phụ huynh/gia sư tăng");
check(after.matching.connected >= before.matching.connected + 1, "số cặp đã kết nối tăng", { before: before.matching.connected, after: after.matching.connected });
check(after.activity.at(-1).newAccounts >= before.activity.at(-1).newAccounts + 2, "hoạt động hôm nay ghi nhận 2 tài khoản mới");

// ================================================================ 10. Đăng xuất
section("10. Đăng xuất");
is(await parent.post("/auth/logout", {}), 200, "phụ huynh đăng xuất");
is(await parent.get("/students"), 401, "sau đăng xuất phiên bị thu hồi");
finish();
