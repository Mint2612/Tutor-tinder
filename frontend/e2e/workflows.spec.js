import { test, expect } from "@playwright/test";

const mailpit = process.env.E2E_MAILPIT_URL || "http://127.0.0.1:38025";
const password = "browser-test-password-123";
const survey = {
  goal: "foundation",
  learning: "practice",
  difficulty: "understanding",
  pace: "step_by_step",
  support: "feedback",
  lesson: "examples",
};

async function createStudent(context, data) {
  const csrf = await (await context.get("/api/auth/csrf")).json();
  const response = await context.post("/api/students", {
    data,
    headers: { "X-XSRF-TOKEN": csrf.token },
  });
  expect(response.ok()).toBeTruthy();
  return response.json();
}
async function emailLink(request, email, action) {
  let link;
  await expect
    .poll(async () => {
      const inbox = await (
        await request.get(`${mailpit}/api/v1/messages`)
      ).json();
      const message = inbox.messages.find((item) =>
        item.To.some((recipient) => recipient.Address === email),
      );
      if (!message) return false;
      const content = await (
        await request.get(`${mailpit}/api/v1/message/${message.ID}`)
      ).json();
      link = content.Text?.match(
        new RegExp(`https?://[^\\s]+/#/${action}\\?[^\\s]+`),
      )?.[0];
      return !!link;
    })
    .toBe(true);
  return link;
}
async function login(page, email, pass = password) {
  await page.goto("/#/login");
  await page.getByLabel("Email", { exact: true }).fill(email);
  await page.getByLabel("Mật khẩu", { exact: true }).fill(pass);
  await page.getByRole("button", { name: "Đăng nhập →", exact: true }).click();
  await expect(
    page.getByRole("button", { name: "Đăng xuất", exact: true }),
  ).toBeVisible();
}
async function logout(page) {
  await page.getByRole("button", { name: "Đăng xuất", exact: true }).click();
  await expect(
    page.getByRole("button", { name: "Đăng xuất", exact: true }),
  ).toHaveCount(0);
}
async function swipeRight(page, mobile) {
  const visual = page.locator(".matching-card h2");
  await visual.scrollIntoViewIfNeeded();
  const box = await visual.boundingBox();
  const x = box.x + box.width / 2,
    y = box.y + box.height / 2;
  if (mobile) {
    const session = await page.context().newCDPSession(page);
    await session.send("Input.dispatchTouchEvent", {
      type: "touchStart",
      touchPoints: [{ x, y }],
    });
    for (let i = 1; i <= 8; i++)
      await session.send("Input.dispatchTouchEvent", {
        type: "touchMove",
        touchPoints: [{ x: x + i * 14, y }],
      });
    await session.send("Input.dispatchTouchEvent", {
      type: "touchEnd",
      touchPoints: [],
    });
    await session.detach();
  } else {
    await page.mouse.move(x, y);
    await page.mouse.down();
    await page.mouse.move(x + 112, y, { steps: 8 });
    await page.mouse.up();
  }
}
async function register(page, request, role, email, displayName) {
  await page.goto("/#/register");
  await page.getByLabel("Email", { exact: true }).fill(email);
  // The name typed at registration is the public display name.
  await page
    .getByLabel("Tên hiển thị")
    .fill(
      displayName ??
        (role === "PARENT" ? "Phụ huynh kiểm thử" : "Gia sư trình duyệt"),
    );
  await page.getByLabel("Số điện thoại").fill("+84901234567");
  await page.getByLabel("Bạn tham gia với vai trò").selectOption(role);
  await page
    .getByLabel("Mật khẩu (12–72 ký tự)", { exact: true })
    .fill(password);
  await page
    .getByRole("button", { name: "Tạo tài khoản →", exact: true })
    .click();
  if (role === "PARENT") {
    // Parents need no verification: registering signs them straight in.
    await expect(
      page.getByRole("button", { name: "Đăng xuất", exact: true }),
    ).toBeVisible();
    return;
  }
  await expect(
    page.getByRole("heading", { name: "Xác minh tài khoản", exact: true }),
  ).toBeVisible();
  await page.goto(await emailLink(request, email, "verify"));
  await expect(page).toHaveURL(/#\/verify$/);
  await page.getByRole("button", { name: "Xác minh", exact: true }).click();
  await expect(
    page.getByRole("heading", { name: "Chào bạn trở lại" }),
  ).toBeVisible();
  await login(page, email);
}

test("matching, profile management, account deletion and password reset", async ({
  page,
  browser,
  request,
}, testInfo) => {
  const errors = [];
  page.on("pageerror", (error) => errors.push(error.message));
  const suffix = `${testInfo.project.name}-${Date.now()}`;
  const parentEmail = `parent-${suffix}@example.test`;
  const tutorEmail = `tutor-${suffix}@example.test`;

  await register(page, request, "TUTOR", tutorEmail, `Gia sư ${suffix}`);
  await expect(
    page.getByRole("heading", { name: "Hồ sơ giảng dạy", exact: true }),
  ).toBeVisible();
  // The profile asks for the legal name (for the admin's identity check), not the display name.
  await page
    .getByLabel("Họ và tên (đúng như giấy tờ)", { exact: true })
    .fill("Nguyễn Văn Kiểm Thử");
  await page.getByLabel("Tỉnh / thành phố", { exact: true }).fill("Hà Nội");
  await page
    .getByLabel("Học vấn, bằng cấp và kinh nghiệm")
    .fill("Đại học, kinh nghiệm dạy Toán.");
  await page
    .getByLabel("Phương pháp giảng dạy")
    .fill("Kiên nhẫn, thực hành từng bước.");
  // A new time slot defaults to Monday 18:00-20:00, which overlaps the parent's own slot.
  await page
    .getByRole("button", { name: "+ Thêm thời gian rảnh", exact: true })
    .click();
  await page.getByRole("button", { name: "Lưu hồ sơ", exact: true }).click();
  await page.getByLabel("Tải bằng chứng danh tính").setInputFiles({
    name: "identity.pdf",
    mimeType: "application/pdf",
    buffer: Buffer.from("%PDF-1.4\nFictional browser test evidence\n%%EOF"),
  });
  await expect(
    page.getByRole("button", { name: "Gửi hồ sơ để duyệt", exact: true }),
  ).toBeEnabled();
  await page
    .getByRole("button", { name: "Gửi hồ sơ để duyệt", exact: true })
    .click();
  await expect(page.getByText("Đang chờ duyệt", { exact: true })).toBeVisible();
  const tutorProfile = await (
    await page.request.get("/api/tutor/profile")
  ).json();

  const adminContext = await browser.newContext({
    baseURL: testInfo.project.use.baseURL || "http://127.0.0.1:33000",
  });
  const admin = await adminContext.newPage();
  await login(admin, "e2e-admin@example.test", "e2e-admin-password-12345");
  const card = admin.locator("article").filter({
    has: admin.getByRole("heading", {
      name: `Gia sư ${suffix}`,
      exact: true,
    }),
  });
  await card
    .getByRole("button", { name: "Xem hồ sơ & giấy tờ", exact: true })
    .click();
  // The admin sees the legal name to compare it with the document.
  await expect(
    admin.getByText("Họ và tên khai báo (phải trùng giấy tờ):"),
  ).toContainText("Nguyễn Văn Kiểm Thử");
  await expect(
    admin.getByRole("button", {
      name: "Phê duyệt sau khi kiểm tra",
      exact: true,
    }),
  ).toBeDisabled();
  const download = admin.waitForEvent("download");
  await admin
    .getByRole("button", { name: "Tải & xem identity.pdf", exact: true })
    .click();
  await download;
  await admin
    .getByRole("button", { name: "Phê duyệt sau khi kiểm tra", exact: true })
    .click();
  await expect(card.getByText("Đã xác minh", { exact: true })).toBeVisible();
  // A profile change withdraws approval, then the new submission needs a fresh evidence read.
  await page.getByLabel("Phương pháp giảng dạy").fill("Phương pháp mới cần quản trị viên duyệt lại.");
  await page.getByRole("button", { name: "Lưu hồ sơ", exact: true }).click();
  await expect(page.getByRole("button", { name: "Gửi hồ sơ để duyệt", exact: true })).toBeVisible();
  await page.getByRole("button", { name: "Gửi hồ sơ để duyệt", exact: true }).click();
  await expect(page.getByText("Đang chờ duyệt", { exact: true })).toBeVisible();
  await admin.reload();
  await card.getByRole("button", { name: "Xem hồ sơ & giấy tờ", exact: true }).click();
  await expect(admin.getByRole("button", { name: "Phê duyệt sau khi kiểm tra", exact: true })).toBeDisabled();
  const reread = admin.waitForEvent("download");
  await admin.getByRole("button", { name: "Tải & xem identity.pdf", exact: true }).click();
  await reread;
  await admin.getByRole("button", { name: "Phê duyệt sau khi kiểm tra", exact: true }).click();
  await expect(card.getByText("Đã xác minh", { exact: true })).toBeVisible();
  await adminContext.close();

  await logout(page);
  await register(page, request, "PARENT", parentEmail);
  await page
    .getByRole("button", { name: "+ Thêm hồ sơ học sinh", exact: true })
    .click();
  await page
    .getByLabel("Tên gọi của con", { exact: true })
    .fill("Con kiểm thử");
  await page.getByLabel("Tỉnh / thành phố", { exact: true }).fill("Hà Nội");
  const needs = page.getByLabel(
    "Mục tiêu, khó khăn và điều bạn muốn gia sư hiểu",
  );
  // Contact details do not belong in text tutors can read.
  await needs.fill("Gọi phụ huynh theo số 0903 123 456");
  await expect(page.getByText("không được ghi số điện thoại")).toBeVisible();
  await needs.fill("Củng cố nền tảng Toán.");
  await expect(page.getByText("không được ghi số điện thoại")).toHaveCount(0);
  const selectQuestions = [
    "Mục tiêu học tập chính?",
    "Con tiếp thu tốt nhất khi nào?",
    "Con đang cần hỗ trợ điều gì?",
    "Nhịp học phù hợp với con?",
    "Mức độ hướng dẫn con mong muốn?",
    "Hoạt động học con yêu thích?",
  ];
  for (const [index, value] of Object.values(survey).entries())
    await page
      .getByLabel(selectQuestions[index], { exact: false })
      .selectOption(value);
  // A new time slot defaults to Monday 18:00-20:00, which overlaps the tutor's slot.
  await page
    .getByRole("button", { name: "+ Thêm thời gian rảnh", exact: true })
    .click();
  await page.getByRole("checkbox", { name: /Tôi là phụ huynh/ }).check();
  await page
    .getByRole("button", { name: "Lưu hồ sơ & khảo sát", exact: true })
    .click();
  await expect(
    page.getByRole("heading", { name: "Con kiểm thử", exact: true }),
  ).toBeVisible();
  await page.getByRole("button", { name: /Khám phá gia sư cho/ }).click();
  await expect(page.locator(".matching-card")).toBeVisible();
  // Older fictional tutors from a local rerun may still be eligible; skip them persistently.
  for (let i = 0; i < 50; i++) {
    await expect(page.locator(".matching-card h2")).toBeVisible();
    const previousTitle = await page.locator(".matching-card h2").textContent();
    if (previousTitle.includes(suffix)) break;
    await page.getByRole("button", { name: "Bỏ qua", exact: true }).click();
    await expect(page.locator(".matching-card h2")).not.toHaveText(previousTitle);
  }
  await expect(page.locator(".matching-card h2")).toHaveText(`Gia sư ${suffix}`);
  // Parents only ever see the display name, never the legal name.
  await expect(page.getByText("Nguyễn Văn Kiểm Thử")).toHaveCount(0);
  await expect(page.locator(".matching-card").getByText("✦ Đề xuất", { exact: true })).toBeVisible();
  await swipeRight(page, testInfo.project.name === "mobile");
  await expect(page.getByText("Đã gửi sự quan tâm. Chờ bên kia cùng OK để tạo Match.", { exact: true })).toBeVisible();
  await expect(page.getByRole("dialog")).toHaveCount(0);
  await page.getByRole("button", { name: "Đã OK & Match", exact: true }).click();
  await expect(page.locator(".matching-card h2")).toHaveText(`Gia sư ${suffix}`);
  await page.reload();
  await expect(page.getByText("Bạn đã OK. Đang chờ bên kia phản hồi.", { exact: true })).toBeVisible();
  expect((await (await page.request.get("/api/lessons")).json()).length).toBe(0);
  const originalStudent = (await (await page.request.get("/api/students")).json())[0];

  const tutorContext = await browser.newContext({ baseURL: "http://127.0.0.1:33000" });
  const tutorPage = await tutorContext.newPage();
  await login(tutorPage, tutorEmail);
  await tutorPage.getByRole("button", { name: "Tìm học sinh", exact: true }).click();
  await expect(tutorPage.locator(".matching-card h2")).toHaveText("Học sinh lớp 6");
  await expect(tutorPage.locator(".matching-card").getByText("♡ Quan tâm bạn", { exact: true })).toBeVisible();
  const tutorFeed = await (await tutorPage.request.get("/api/tutor/matching")).json();
  expect(tutorFeed.cards[0].studentId).toBe(originalStudent.id);
  const anonymous = JSON.stringify(tutorFeed.cards[0]);
  expect(anonymous).not.toContain(parentEmail);
  expect(anonymous).not.toContain("Con kiểm thử");
  expect(anonymous).not.toContain("Phụ huynh kiểm thử");
  await tutorPage.getByRole("button", { name: "OK — quan tâm", exact: true }).click();
  await expect(tutorPage.getByText("Hai bên đã cùng OK. Bạn có một Match mới!", { exact: true })).toBeVisible();
  await tutorPage.getByRole("button", { name: "Đã OK & Match", exact: true }).click();
  await expect(tutorPage.locator(".matching-card").getByText("✓ Đã Match", { exact: true })).toBeVisible();
  await page.bringToFront();
  await expect(page.locator(".matching-card").getByText("✓ Đã Match", { exact: true })).toBeVisible();
  await expect(page.getByLabel("Thông báo Match")).toBeVisible();
  expect((await (await page.request.get("/api/lessons")).json()).length).toBe(0);

  // Sprint 5: detail remains anonymous until both participants confirm connection.
  const detailButton = { name: "Xem hồ sơ & xác nhận kết nối", exact: true };
  await page.getByRole("button", detailButton).click();
  const parentDetail = page.getByRole("dialog", { name: "Hồ sơ chi tiết" });
  await expect(parentDetail.getByRole("heading", { name: `Gia sư ${suffix}` })).toBeVisible();
  await expect(parentDetail).not.toContainText(tutorEmail);
  await parentDetail.getByRole("button", { name: "Xác nhận kết nối", exact: true }).click();
  await expect(parentDetail.getByRole("status")).toContainText("Đang chờ bên kia");
  await expect(parentDetail).not.toContainText(tutorEmail);
  await parentDetail.getByRole("button", { name: "Đóng cửa sổ", exact: true }).click();
  await tutorPage.getByRole("button", detailButton).click();
  const tutorDetail = tutorPage.getByRole("dialog", { name: "Hồ sơ chi tiết" });
  await expect(tutorDetail.getByRole("heading", { name: "Học sinh lớp 6" })).toBeVisible();
  await expect(tutorDetail).not.toContainText(parentEmail);
  await expect(tutorDetail).not.toContainText("Con kiểm thử");
  await tutorDetail.getByRole("button", { name: "Xác nhận kết nối", exact: true }).click();
  await expect(tutorDetail.locator(".contact")).toContainText(parentEmail);
  await tutorDetail.getByRole("button", { name: "Đóng cửa sổ", exact: true }).click();
  await page.getByRole("button", detailButton).click();
  await expect(parentDetail.locator(".contact")).toContainText(tutorEmail);
  await expect(parentDetail.getByRole("button", { name: "Xác nhận kết nối", exact: true })).toHaveCount(0);
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true);
  // Sprint 5/6: proposal, a recoverable network error, counterproposal, confirmation and cancellation.
  const localTime = (days) => {
    const d = new Date(Date.now() + days * 86400000);
    return new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 16);
  };
  await parentDetail.getByLabel("Giờ đề xuất 1", { exact: true }).fill(localTime(2));
  await parentDetail.getByRole("button", { name: "Thêm giờ đề xuất", exact: true }).click();
  await parentDetail.getByLabel("Giờ đề xuất 2", { exact: true }).fill(localTime(3));
  await page.route("**/api/trials", (route) => route.abort("failed"), { times: 1 });
  await parentDetail.getByRole("button", { name: "Gửi đề xuất học thử", exact: true }).click();
  await expect(parentDetail.getByRole("alert")).toContainText("Không thể kết nối");
  await parentDetail.getByRole("button", { name: "Gửi đề xuất học thử", exact: true }).click();
  await expect(parentDetail.getByText("Chờ phản hồi", { exact: true })).toBeVisible();
  await tutorPage.getByRole("button", detailButton).click();
  await tutorDetail.getByRole("button", { name: "Đề xuất giờ khác", exact: true }).click();
  await tutorDetail.getByLabel("Giờ đề xuất 1", { exact: true }).fill(localTime(4));
  await tutorDetail.getByRole("button", { name: "Gửi đề xuất học thử", exact: true }).click();
  await expect(tutorDetail.getByText("Bạn đã đề xuất; đang chờ bên kia phản hồi.", { exact: true })).toBeVisible();
  await parentDetail.getByRole("button", { name: "Cập nhật lịch", exact: true }).click();
  await parentDetail.getByRole("combobox", { name: "Chọn giờ học thử", exact: true }).selectOption({ index: 1 });
  await parentDetail.getByRole("button", { name: "Chốt giờ học thử", exact: true }).click();
  await expect(parentDetail.getByText("Đã chốt", { exact: true })).toBeVisible();
  await parentDetail.screenshot({ path: `test-results/sprint56-trial-${testInfo.project.name}.png` });
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true);
  await tutorDetail.getByRole("button", { name: "Cập nhật lịch", exact: true }).click();
  await expect(tutorDetail.getByText("Đã chốt", { exact: true })).toBeVisible();
  await tutorDetail.getByRole("button", { name: "Hủy lịch học thử", exact: true }).click();
  await expect(tutorDetail.getByRole("button", { name: "Xác nhận hủy lịch", exact: true })).toBeDisabled();
  await tutorDetail.getByLabel("Lý do hủy lịch", { exact: true }).fill("Đổi kế hoạch kiểm thử");
  await tutorDetail.getByRole("button", { name: "Xác nhận hủy lịch", exact: true }).click();
  await expect(tutorDetail.getByText(/Lý do: Đổi kế hoạch kiểm thử/)).toBeVisible();
  await tutorDetail.getByRole("button", { name: "Đóng cửa sổ", exact: true }).click();
  await parentDetail.getByRole("button", { name: "Cập nhật lịch", exact: true }).click();
  await expect(parentDetail.getByText(/Lý do: Đổi kế hoạch kiểm thử/)).toBeVisible();
  await parentDetail.getByRole("button", { name: "Đóng cửa sổ", exact: true }).click();

  const secondStudent = await createStudent(page.request, {
    nickname: "Học sinh thứ hai", grade: 6, subjects: ["Toán"], province: "Hà Nội",
    mode: "ONLINE", budget: 200000, availability: ["1_EVENING"],
    needs: "Củng cố nền tảng", survey, consent: true,
  });
  await page.reload();
  await page.getByRole("combobox", { name: "Hồ sơ học sinh", exact: true }).selectOption(secondStudent.id);
  await expect(page.getByRole("heading", { name: "Chưa có lựa chọn đang chờ hoặc Match", exact: true })).toBeVisible();
  await page.getByRole("button", { name: "Xem Match", exact: true }).click();
  await expect(page.getByRole("combobox", { name: "Hồ sơ học sinh", exact: true })).toHaveValue(originalStudent.id);
  await expect(page.locator(".matching-card").getByText("✓ Đã Match", { exact: true })).toBeVisible();
  await page.screenshot({ path: `test-results/sprint4-${testInfo.project.name}.png`, fullPage: true });
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true);
  // Exceed the former daily OK limit through the API; the UI must stay enabled.
  for (let i = 0; i < 12; i++) {
    const s = await createStudent(page.request, {
      nickname: `Hồ sơ giới hạn ${i}`, grade: 6, subjects: ["Toán"], province: "Hà Nội",
      mode: "ONLINE", budget: 200000, availability: ["1_EVENING"], needs: "Củng cố kiến thức", survey, consent: true,
    });
    const csrf = await (await tutorPage.request.get("/api/auth/csrf")).json();
    const response = await tutorPage.request.post("/api/matching/actions", {
      data: { studentId: s.id, tutorId: tutorProfile.id, interested: true },
      headers: { "X-XSRF-TOKEN": csrf.token },
    });
    expect(response.ok()).toBeTruthy();
  }
  await tutorPage.getByRole("button", { name: "Tìm học sinh", exact: true }).click();
  await expect(tutorPage.getByRole("button", { name: "OK — quan tâm", exact: true })).toBeEnabled();
  await expect(tutorPage.getByRole("button", { name: "Bỏ qua", exact: true })).toBeEnabled();
  const capped = await (await tutorPage.request.get("/api/tutor/matching")).json();
  expect(capped).not.toHaveProperty("limits");
  await tutorPage.reload();
  await expect(tutorPage.getByRole("button", { name: "OK — quan tâm", exact: true })).toBeEnabled();
  expect((await (await tutorPage.request.get("/api/tutor/matching")).json()).cards[0].studentId).toBe(capped.cards[0].studentId);
  const skipResponse = tutorPage.waitForResponse((response) => response.url().endsWith("/api/matching/actions") && response.request().method() === "POST");
  await tutorPage.getByRole("button", { name: "Bỏ qua", exact: true }).click();
  expect((await skipResponse).ok()).toBeTruthy();
  await tutorContext.close();

  await logout(page);
  await page.goto("/#/forgot");
  await page.getByLabel("Email", { exact: true }).fill(parentEmail);
  await page
    .getByRole("button", { name: "Gửi hướng dẫn đặt lại", exact: true })
    .click();
  await page.goto(await emailLink(request, parentEmail, "reset"));
  await page
    .getByLabel("Mật khẩu mới (12–72 ký tự)", { exact: true })
    .fill("reset-browser-password-123");
  await page
    .getByRole("button", { name: "Lưu mật khẩu mới", exact: true })
    .click();
  await login(page, parentEmail, "reset-browser-password-123");
  // Profile management runs on these fictional accounts before deleting them at the end.
  const managedStudent = await createStudent(page.request, {
    nickname: "Con quản lý", grade: 6, subjects: ["Toán"], province: "Hà Nội", mode: "ONLINE",
    budget: 200000, availability: ["1_EVENING"], needs: "Củng cố tư duy Toán", survey, consent: true,
  });
  const managementContext = await browser.newContext({ baseURL: "http://127.0.0.1:33000",
    ...(testInfo.project.name === "mobile" ? { viewport: { width: 360, height: 800 }, isMobile: true, hasTouch: true } : {}) });
  const teacherManagement = await managementContext.newPage();
  await login(teacherManagement, tutorEmail);
  const accepting = teacherManagement.getByRole("switch", { name: "Nhận học sinh mới", exact: true });
  await accepting.uncheck();
  await expect(teacherManagement.getByText("Đã tạm ẩn khỏi đề xuất. Các Match đã có được giữ lại.", { exact: true })).toBeVisible();
  await teacherManagement.reload();
  await expect(accepting).not.toBeChecked();
  expect((await (await page.request.get(`/api/students/${managedStudent.id}/matching`)).json()).cards.some((c) => c.tutorId === tutorProfile.id)).toBe(false);
  await accepting.check();
  await expect(teacherManagement.getByText("Đã bật nhận học sinh. Quyền hiển thị vẫn phụ thuộc kết quả duyệt và admin.", { exact: true })).toBeVisible();
  expect((await (await page.request.get(`/api/students/${managedStudent.id}/matching`)).json()).cards.some((c) => c.tutorId === tutorProfile.id)).toBe(true);

  await page.reload();
  let managedCard = page.locator("article.student").filter({ has: page.getByRole("heading", { name: "Con quản lý", exact: true }) });
  await managedCard.getByRole("button", { name: "Ẩn hồ sơ con", exact: true }).click();
  await expect(managedCard.getByRole("button", { name: "Bật hiển thị hồ sơ con", exact: true })).toBeVisible();
  await page.reload();
  await expect(managedCard.getByRole("button", { name: "Bật hiển thị hồ sơ con", exact: true })).toBeVisible();
  expect((await (await teacherManagement.request.get("/api/tutor/matching")).json()).cards.some((c) => c.studentId === managedStudent.id)).toBe(false);
  await managedCard.getByRole("button", { name: "Bật hiển thị hồ sơ con", exact: true }).click();
  await expect(managedCard.getByRole("button", { name: "Ẩn hồ sơ con", exact: true })).toBeVisible();
  await managedCard.getByRole("button", { name: "Chỉnh sửa hồ sơ & khảo sát", exact: true }).click();
  await page.getByLabel("Tên gọi của con", { exact: true }).fill("Con quản lý đã sửa");
  await page.getByRole("checkbox", { name: /Tôi là phụ huynh/ }).check();
  await page.getByRole("button", { name: "Lưu hồ sơ & khảo sát", exact: true }).click();
  managedCard = page.locator("article.student").filter({ has: page.getByRole("heading", { name: "Con quản lý đã sửa", exact: true }) });
  await managedCard.getByRole("button", { name: "Xóa hồ sơ con", exact: true }).click();
  let deletion = page.getByRole("dialog");
  await expect(deletion.getByRole("button", { name: "Xác nhận xóa", exact: true })).toBeDisabled();
  await deletion.getByRole("button", { name: "Hủy", exact: true }).click();
  await expect(managedCard).toBeVisible();
  await managedCard.getByRole("button", { name: "Xóa hồ sơ con", exact: true }).click();
  await deletion.getByLabel("Nhập XÓA để xác nhận", { exact: true }).fill("XÓA");
  await deletion.getByRole("button", { name: "Xác nhận xóa", exact: true }).click();
  await expect(managedCard).toHaveCount(0);
  expect((await (await page.request.get("/api/students")).json()).some((s) => s.id === managedStudent.id)).toBe(false);

  await teacherManagement.getByRole("button", { name: "Xóa hồ sơ giảng dạy", exact: true }).click();
  const teacherDialog = teacherManagement.getByRole("dialog");
  await teacherDialog.getByLabel("Mật khẩu hiện tại để xác nhận xóa").fill("wrong-password");
  await teacherDialog.getByLabel("Nhập XÓA để xác nhận").fill("XÓA");
  await teacherDialog.getByRole("button", { name: "Xác nhận xóa", exact: true }).click();
  await expect(teacherDialog.getByRole("alert")).toContainText("Mật khẩu hiện tại không đúng");
  await teacherDialog.getByLabel("Mật khẩu hiện tại để xác nhận xóa").fill(password);
  await teacherDialog.getByRole("button", { name: "Xác nhận xóa", exact: true }).click();
  await expect(teacherManagement.getByRole("heading", { name: "Trạng thái nhận học sinh", exact: true })).toHaveCount(0);
  expect((await teacherManagement.request.get("/api/tutor/profile")).status()).toBe(404);
  await teacherManagement.screenshot({ path: `test-results/management-${testInfo.project.name}.png`, fullPage: true });
  await teacherManagement.getByRole("button", { name: "Xóa tài khoản của tôi", exact: true }).click();
  await teacherDialog.getByLabel("Mật khẩu hiện tại để xác nhận xóa").fill(password);
  await teacherDialog.getByLabel("Nhập XÓA để xác nhận").fill("XÓA");
  await teacherDialog.getByRole("button", { name: "Xác nhận xóa", exact: true }).click();
  await expect(teacherManagement.getByRole("button", { name: "Đăng xuất", exact: true })).toHaveCount(0);
  expect((await teacherManagement.request.get("/api/auth/me")).status()).toBe(401);

  await page.getByRole("button", { name: "Tài khoản", exact: true }).click();
  await page.getByRole("button", { name: "Xóa tài khoản của tôi", exact: true }).click();
  deletion = page.getByRole("dialog");
  await deletion.getByLabel("Mật khẩu hiện tại để xác nhận xóa").fill("reset-browser-password-123");
  await deletion.getByLabel("Nhập XÓA để xác nhận").fill("XÓA");
  await deletion.getByRole("button", { name: "Xác nhận xóa", exact: true }).click();
  await expect(page.getByRole("button", { name: "Đăng xuất", exact: true })).toHaveCount(0);
  expect((await page.request.get("/api/auth/me")).status()).toBe(401);
  await managementContext.close();
  expect(errors).toEqual([]);
});

test("parent signs in right after registering and a duplicate email gets sign-in guidance", async ({
  page,
  request,
}, testInfo) => {
  const email = `solo-parent-${testInfo.project.name}-${Date.now()}@example.test`;
  await register(page, request, "PARENT", email);
  await expect(
    page.getByRole("heading", { name: "Hiểu con hơn, tìm đúng người." }),
  ).toBeVisible();
  await logout(page);

  await page.goto("/#/register");
  await page.getByLabel("Email", { exact: true }).fill(email);
  await page.getByLabel("Tên hiển thị").fill("Người đăng ký lại");
  await page.getByLabel("Số điện thoại").fill("+84900000000");
  await page.getByLabel("Bạn tham gia với vai trò").selectOption("PARENT");
  await page
    .getByLabel("Mật khẩu (12–72 ký tự)", { exact: true })
    .fill("another-password-123");
  await page
    .getByRole("button", { name: "Tạo tài khoản →", exact: true })
    .click();
  await expect(page.getByRole("alert")).toContainText("đã có tài khoản");
  await expect(
    page.getByRole("button", { name: "Đăng xuất", exact: true }),
  ).toHaveCount(0);
  // The original password still works; the new one never did.
  await login(page, email);
});

test("role guards and network failures provide a usable recovery path", async ({
  page,
}) => {
  await page.goto("/#/admin");
  await expect(
    page.getByRole("heading", { name: "Chào bạn trở lại", exact: true }),
  ).toBeVisible();
  await page.route("**/api/auth/login", (route) => route.abort());
  await page.getByLabel("Email", { exact: true }).fill("missing@example.test");
  await page.getByLabel("Mật khẩu", { exact: true }).fill(password);
  await page.getByRole("button", { name: "Đăng nhập →", exact: true }).click();
  await expect(page.getByRole("alert")).toContainText("Không thể kết nối");
  await expect(
    page.getByRole("button", { name: "Đăng nhập →", exact: true }),
  ).toBeEnabled();
});
