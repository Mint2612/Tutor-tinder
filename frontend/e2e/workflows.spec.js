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

test("Sprint 4: two-way anonymous cards, swipe, Match, persisted choices and password reset", async ({
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
  await page.reload();
  await expect(page.locator(".matching-card").getByText("✓ Đã Match", { exact: true })).toBeVisible();
  await expect(page.getByLabel("Thông báo Match")).toBeVisible();
  expect((await (await page.request.get("/api/lessons")).json()).length).toBe(0);

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
  // Fill the tutor's remaining daily budget through the real API, then verify the disabled UI.
  for (let i = 0; i < 9; i++) {
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
  await expect(tutorPage.getByRole("button", { name: "OK — quan tâm", exact: true })).toBeDisabled();
  await expect(tutorPage.getByRole("button", { name: "Bỏ qua", exact: true })).toBeEnabled();
  const capped = await (await tutorPage.request.get("/api/tutor/matching")).json();
  expect(capped.limits.totalRemaining).toBe(0);
  await tutorPage.reload();
  await expect(tutorPage.getByRole("button", { name: "OK — quan tâm", exact: true })).toBeDisabled();
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
