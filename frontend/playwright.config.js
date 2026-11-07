import { defineConfig, devices } from "@playwright/test";

export default defineConfig({
  testDir: "./e2e",
  fullyParallel: false,
  workers: 1,
  timeout: 120000,
  expect: { timeout: 10000 },
  forbidOnly: !!process.env.CI,
  retries: 0,
  reporter: "list",
  use: {
    actionTimeout: 15000,
    baseURL: process.env.E2E_BASE_URL || "http://127.0.0.1:33000",
    screenshot: "only-on-failure",
    // Tests handle verification secrets; do not retain network traces containing email links.
    trace: "off",
  },
  projects: [
    { name: "desktop", use: { ...devices["Desktop Chrome"] } },
    {
      name: "mobile",
      use: { ...devices["Pixel 7"], viewport: { width: 360, height: 800 } },
    },
  ],
});
