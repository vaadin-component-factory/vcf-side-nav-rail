/*
 * One-off tool to capture the README screenshots from the demo's
 * /screenshot route. Lives next to the e2e tests so it can reuse the
 * @playwright/test dependency without its own package.json.
 *
 * Usage: start the demo (./mvnw -pl demo spring-boot:run) and from
 * this directory run `node capture-screenshots.cjs`. Outputs go to
 * /workspace/docs/screenshots/. Re-run after design changes that
 * should refresh the README images.
 */
const { chromium } = require('@playwright/test');

(async () => {
  const browser = await chromium.launch();
  const context = await browser.newContext({
    viewport: { width: 900, height: 500 },
    deviceScaleFactor: 2,
  });
  const page = await context.newPage();

  async function hideDevTools() {
    await page.evaluate(() => {
      for (const el of document.querySelectorAll('vaadin-dev-tools, copilot-main, vaadin-connection-indicator, vite-plugin-checker-error-overlay')) {
        el.style.display = 'none';
      }
    });
  }

  // Rail OFF — crop to the rail's area plus a small breathing strip on the right.
  await page.goto('http://localhost:8080/screenshot');
  await page.waitForSelector('vaadin-side-nav-item[path="dashboard"]');
  await page.waitForTimeout(800);
  await hideDevTools();
  await page.screenshot({
    path: '/workspace/docs/screenshots/rail-off.png',
    clip: { x: 0, y: 0, width: 280, height: 280 },
  });
  console.log('captured rail-off.png');

  // Rail ON with popover open on Code — crop to rail + popover.
  await page.evaluate(() => {
    const rail = document.querySelector('vaadin-side-nav#screenshot-rail');
    rail.setAttribute('theme', 'rail');
  });
  await page.waitForTimeout(500);
  await page.locator('vaadin-side-nav-item[path="code"]').hover();
  await page.waitForSelector('vaadin-popover-overlay[opened]', { timeout: 5000 });
  await page.waitForTimeout(800);
  await hideDevTools();
  await page.screenshot({
    path: '/workspace/docs/screenshots/rail-on.png',
    clip: { x: 0, y: 0, width: 230, height: 280 },
  });
  console.log('captured rail-on.png');

  await browser.close();
})();
