/*
 * One-off tool to capture the README screenshots from the demo's
 * /screenshot route. Lives next to the e2e tests so it can reuse the
 * @playwright/test dependency without its own package.json.
 *
 * Produces 4 side-by-side composite PNGs in /workspace/docs/screenshots/
 * via Playwright (capture) + ImageMagick `convert +append` (composite):
 *
 *   1-modes.png     — rail off vs rail on (no popovers)
 *   2-popovers.png  — hover popover in normal mode vs rail mode
 *   3-children.png  — children inline (normal expand) vs in popover (rail)
 *   4-nested.png    — nested children expanded inside the popover (rail)
 *
 * Usage: start the demo (./mvnw -pl demo spring-boot:run) and from this
 * directory run `node capture-screenshots.cjs`. Re-run after design
 * changes that should refresh the README images.
 */
const { chromium } = require('@playwright/test');
const { execFileSync } = require('child_process');
const fs = require('fs');
const path = require('path');

const OUT = '/workspace/docs/screenshots';
const TMP = '/tmp/screenshot-frames';

function ensureDir(d) { fs.mkdirSync(d, { recursive: true }); }

function composite(left, right, out) {
    // ImageMagick: side-by-side, 12px transparent gutter between cells.
    execFileSync('convert', [
        path.join(TMP, left),
        '-background', 'white',
        '-splice', '12x0',
        path.join(TMP, right),
        '+append',
        path.join(OUT, out),
    ]);
    console.log('composed', out);
}

async function setRail(page, on) {
    await page.evaluate((rail) => {
        const el = document.querySelector('vaadin-side-nav#screenshot-rail');
        if (rail) el.setAttribute('theme', 'rail');
        else el.removeAttribute('theme');
    }, on);
    await page.waitForTimeout(400);
}

async function expandItem(page, scope, path) {
    // Set .expanded directly on the side-nav-item; click would also navigate.
    await page.evaluate(({ scope, path }) => {
        const root = scope ? document.querySelector(scope) : document;
        const item = root.querySelector(`vaadin-side-nav-item[path="${path}"]`);
        item.expanded = true;
    }, { scope, path });
    await page.waitForTimeout(300);
}

async function collapseAll(page) {
    await page.evaluate(() => {
        for (const it of document.querySelectorAll('vaadin-side-nav-item')) {
            it.expanded = false;
        }
    });
    await page.waitForTimeout(300);
}

async function closeAnyPopover(page) {
    await page.evaluate(() => {
        for (const o of document.querySelectorAll('vaadin-popover')) {
            o.opened = false;
        }
    });
    await page.waitForTimeout(400);
}

async function hideDevTools(page) {
    await page.evaluate(() => {
        for (const el of document.querySelectorAll(
            'vaadin-dev-tools, copilot-main, vaadin-connection-indicator, vite-plugin-checker-error-overlay')) {
            el.style.display = 'none';
        }
    });
}

async function shot(page, name, clip) {
    await hideDevTools(page);
    await page.screenshot({ path: path.join(TMP, name), clip });
    console.log('captured', name);
}

(async () => {
    ensureDir(OUT);
    ensureDir(TMP);

    const browser = await chromium.launch();
    const context = await browser.newContext({
        viewport: { width: 900, height: 500 },
        deviceScaleFactor: 2,
    });
    const page = await context.newPage();

    await page.goto('http://localhost:8080/screenshot');
    await page.waitForSelector('vaadin-side-nav-item[path="dashboard"]');
    await page.waitForTimeout(800);

    // ---------- A — rail off vs rail on (no popovers) ----------
    await collapseAll(page);
    await setRail(page, false);
    await shot(page, 'a-normal.png', { x: 0, y: 0, width: 280, height: 280 });

    await setRail(page, true);
    await shot(page, 'a-rail.png', { x: 0, y: 0, width: 80, height: 280 });

    composite('a-normal.png', 'a-rail.png', '1-modes.png');

    // ---------- B — popover in normal vs rail (collapsed parent, on hover) ----------
    await setRail(page, false);
    await collapseAll(page);
    await page.locator('vaadin-side-nav-item[path="code"]').hover();
    await page.waitForSelector('vaadin-popover-overlay[opened]', { timeout: 5000 });
    await page.waitForTimeout(800);
    await shot(page, 'b-normal.png', { x: 0, y: 0, width: 580, height: 280 });

    await closeAnyPopover(page);
    await setRail(page, true);
    await page.locator('vaadin-side-nav-item[path="code"]').hover();
    await page.waitForSelector('vaadin-popover-overlay[opened]', { timeout: 5000 });
    await page.waitForTimeout(800);
    await shot(page, 'b-rail.png', { x: 0, y: 0, width: 380, height: 280 });

    composite('b-normal.png', 'b-rail.png', '2-popovers.png');

    // ---------- C — children inline (normal expand) vs popover (rail) ----------
    await closeAnyPopover(page);
    await setRail(page, false);
    await collapseAll(page);
    await expandItem(page, '#screenshot-rail', 'code');
    await page.waitForTimeout(400);
    // No hover — show plain inline expansion.
    await page.mouse.move(700, 250);
    await page.waitForTimeout(200);
    await shot(page, 'c-normal.png', { x: 0, y: 0, width: 280, height: 380 });

    await collapseAll(page);
    await setRail(page, true);
    await page.locator('vaadin-side-nav-item[path="code"]').hover();
    await page.waitForSelector('vaadin-popover-overlay[opened]', { timeout: 5000 });
    await page.waitForTimeout(800);
    await shot(page, 'c-rail.png', { x: 0, y: 0, width: 380, height: 380 });

    composite('c-normal.png', 'c-rail.png', '3-children.png');

    // ---------- D — nested children expanded inside the popover (rail) ----------
    await closeAnyPopover(page);
    await collapseAll(page);
    await setRail(page, true);
    await page.locator('vaadin-side-nav-item[path="admin"]').hover();
    await page.waitForSelector('vaadin-popover-overlay[opened]', { timeout: 5000 });
    await page.waitForTimeout(500);
    // Expand Users INSIDE the popover overlay so its children render inline.
    await page.evaluate(() => {
        const overlay = document.querySelector('vaadin-popover-overlay[opened]');
        const users = overlay.querySelector('vaadin-side-nav-item[path="admin/users"]');
        users.expanded = true;
    });
    await page.waitForTimeout(500);
    await shot(page, 'd-rail.png', { x: 0, y: 0, width: 380, height: 380 });

    // D ships standalone (no left-hand "normal" counterpart), so just copy it
    // into the output directory under its final name.
    fs.copyFileSync(path.join(TMP, 'd-rail.png'), path.join(OUT, '4-nested.png'));
    console.log('copied 4-nested.png');

    await browser.close();
})();
