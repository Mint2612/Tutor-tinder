import { test, expect } from "@playwright/test";

async function savedFeed(page) {
  const state = { matched: false, failNext: false };
  await page.route("**/api/**", async (route) => {
    const path = new URL(route.request().url()).pathname;
    if (path === "/api/auth/me") return route.fulfill({ json: {
      id: "refresh-tutor", name: "Gia sư kiểm thử", role: "TUTOR", email: "refresh@example.test",
    } });
    if (path === "/api/matching/notifications") return route.fulfill({ json: [] });
    if (path === "/api/tutor/matching") {
      if (state.failNext) {
        state.failNext = false;
        return route.fulfill({ status: 503, json: { message: "Lỗi tải danh sách kiểm thử" } });
      }
      return route.fulfill({ json: { cards: [], matches: [], paused: false, savedCards: [{
        studentId: "refresh-student", tutorId: "refresh-tutor-profile", tag: "SUGGESTED",
        level: "GOOD", strengths: [], concerns: [], explanation: "Phù hợp điều kiện học tập",
        tutor: null, student: { grade: 6, subjects: ["Toán"], mode: "ONLINE", province: "Hà Nội",
          budget: 200000, availability: ["1_EVENING"], learningPreferences: [], needsSummary: "Củng cố kiến thức" },
        expiresAt: null, canLike: false, status: state.matched ? "MATCHED" : "OPEN", ruleScore: 10,
      }] } });
    }
    return route.fulfill({ status: 404, json: { message: "Không tìm thấy" } });
  });
  await page.clock.install();
  await page.goto("/#/interests");
  await expect(page.getByText("Bạn đã OK. Đang chờ bên kia phản hồi.", { exact: true })).toBeVisible();
  return state;
}

test("saved interest becomes a Match after the other side responds without reloading", async ({ page }) => {
  const state = await savedFeed(page);
  state.matched = true;
  await page.clock.runFor(3000);
  await expect(page.locator(".matching-card").getByText("✓ Đã Match", { exact: true })).toBeVisible();
  await expect(page.getByText("Bạn đã OK. Đang chờ bên kia phản hồi.", { exact: true })).toHaveCount(0);
  await expect(page.getByRole("button", { name: "Xem hồ sơ & xác nhận kết nối", exact: true })).toBeVisible();
});

test("a failed background refresh keeps the saved card and the next read recovers", async ({ page }) => {
  const state = await savedFeed(page);
  state.failNext = true;
  await page.clock.runFor(3000);
  await expect(page.getByRole("alert").filter({ hasText: "Lỗi tải danh sách kiểm thử" })).toBeVisible();
  await expect(page.locator(".matching-card")).toBeVisible();
  state.matched = true;
  await page.clock.runFor(3000);
  await expect(page.locator(".matching-card").getByText("✓ Đã Match", { exact: true })).toBeVisible();
  await expect(page.getByText("Lỗi tải danh sách kiểm thử", { exact: false })).toHaveCount(0);
});
