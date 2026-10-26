const http = require('http');
const { spawn } = require('child_process');
const path = require('path');
const fs = require('fs');
const assert = require('assert');

// 1. Read admin_web.py and extract ADMIN_HTML
const adminWebPath = path.join(__dirname, '../aimentor/admin_web.py');
const pyContent = fs.readFileSync(adminWebPath, 'utf8');

// Extract ADMIN_HTML = """<!DOCTYPE html> ... """
const htmlMatch = pyContent.match(/ADMIN_HTML\s*=\s*"""([\s\S]*?)"""/);
if (!htmlMatch) {
    console.error("FAILED to extract ADMIN_HTML from admin_web.py");
    process.exit(1);
}
let htmlSource = htmlMatch[1];

// Pre-seed localStorage with admin token and intercept alert so test can record alerts
const injectionScript = `
<script>
    localStorage.setItem('admin_token', 'valid_test_token');
    window.__recordedAlerts = [];
    window.__originalAlert = window.alert;
    window.alert = function(msg) {
        window.__recordedAlerts.push(String(msg));
        console.log('[BROWSER ALERT RECORDED]', msg);
    };
</script>
`;
htmlSource = htmlSource.replace('<head>', '<head>' + injectionScript);

let pendingSaveResolvers = [];

// 2. Setup mock HTTP server
const server = http.createServer((req, res) => {
    const url = new URL(req.url, 'http://127.0.0.1:8765');

    // CORS & JSON headers
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Headers', '*');
    res.setHeader('Access-Control-Allow-Methods', '*');

    if (req.method === 'OPTIONS') {
        res.writeHead(204);
        res.end();
        return;
    }

    if (url.pathname === '/') {
        res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
        res.end(htmlSource);
        return;
    }

    if (url.pathname === '/me/account') {
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ username: 'admin_tester', role: 'ADMIN' }));
        return;
    }

    if (url.pathname === '/admin/questions') {
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify([
            {
                id: 'q_browser_01',
                code: 'PHY10_01',
                prompt: 'Chuyển động thẳng biến đổi đều là gì?',
                subjectId: 'physics',
                grade: 10,
                difficulty: 'EASY',
                status: 'PUBLISHED',
                optionA: 'Vận tốc không đổi',
                optionB: 'Gia tốc không đổi',
                optionC: 'Quãng đường không đổi',
                optionD: 'Không có gia tốc',
                correctOption: 'B'
            }
        ]));
        return;
    }

    if (url.pathname === '/admin/quizzes') {
        if (req.method === 'GET') {
            res.writeHead(200, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify([]));
            return;
        }

        if (req.method === 'POST') {
            let body = '';
            req.on('data', chunk => body += chunk);
            req.on('end', () => {
                const parsedBody = JSON.parse(body || '{}');
                console.log(`[SERVER] Received POST /admin/quizzes with status: ${parsedBody.status}`);

                // Check if test wants to delay this response
                if (pendingSaveResolvers.length > 0) {
                    const resolver = pendingSaveResolvers.shift();
                    resolver({ res, body: parsedBody });
                } else {
                    res.writeHead(201, { 'Content-Type': 'application/json' });
                    res.end(JSON.stringify({
                        id: parsedBody.id || 'quiz_auto_id',
                        title: parsedBody.title,
                        status: parsedBody.status
                    }));
                }
            });
            return;
        }
    }

    res.writeHead(404);
    res.end();
});

async function runBrowserTest() {
    await new Promise(r => server.listen(8765, '127.0.0.1', r));
    console.log('[SERVER] Mock web server listening on http://127.0.0.1:8765');

    const chromePath = 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe';
    const profileDir = path.join(process.env.MENTOR_DATA_DIR ? path.dirname(process.env.MENTOR_DATA_DIR) : 'C:/Users/TGPM/.gemini/antigravity/brain/786fc9fc-2b88-4e04-a492-1d22c326451a/scratch', 'chrome_real_test_profile');
    if (!fs.existsSync(profileDir)) fs.mkdirSync(profileDir, { recursive: true });

    const port = 9337;
    const chromeProc = spawn(chromePath, [
        '--headless=new',
        `--remote-debugging-port=${port}`,
        `--user-data-dir=${profileDir}`,
        '--disable-gpu',
        '--no-first-run',
        '--no-default-browser-check'
    ]);

    await new Promise(r => setTimeout(r, 2000));

    let browserWs = null;
    let pageWs = null;

    try {
        const vRes = await fetch(`http://127.0.0.1:${port}/json/version`);
        const vData = await vRes.json();
        console.log(`[CHROME] Connected to browser: ${vData.Browser}`);

        browserWs = new WebSocket(vData.webSocketDebuggerUrl);
        await new Promise(r => browserWs.onopen = r);

        let bId = 1;
        function sendBrowser(method, params = {}) {
            return new Promise((resolve) => {
                const reqId = bId++;
                const handler = (msg) => {
                    const parsed = JSON.parse(msg.data);
                    if (parsed.id === reqId) {
                        browserWs.removeEventListener('message', handler);
                        resolve(parsed.result);
                    }
                };
                browserWs.addEventListener('message', handler);
                browserWs.send(JSON.stringify({ id: reqId, method, params }));
            });
        }

        const targetRes = await sendBrowser('Target.createTarget', { url: 'http://127.0.0.1:8765/' });
        console.log('[CHROME] Target created:', targetRes.targetId);

        pageWs = new WebSocket(`ws://127.0.0.1:${port}/devtools/page/${targetRes.targetId}`);
        await new Promise(r => pageWs.onopen = r);

        let pId = 1;
        function cdp(method, params = {}) {
            return new Promise((resolve, reject) => {
                const id = pId++;
                const handler = (msg) => {
                    const parsed = JSON.parse(msg.data);
                    if (parsed.id === id) {
                        pageWs.removeEventListener('message', handler);
                        if (parsed.error) reject(new Error(parsed.error.message));
                        else resolve(parsed.result);
                    }
                };
                pageWs.addEventListener('message', handler);
                pageWs.send(JSON.stringify({ id, method, params }));
            });
        }

        async function evalInPage(expression) {
            const res = await cdp('Runtime.evaluate', { expression, returnByValue: true, awaitPromise: true });
            return res.result ? res.result.value : null;
        }

        await cdp('Page.enable');
        await cdp('Runtime.enable');

        // Wait for page load and auth check to complete
        await new Promise(r => setTimeout(r, 2000));

        // Switch to Quizzes tab
        await evalInPage(`showSection('quizzes')`);
        await new Promise(r => setTimeout(r, 500));

        // =========================================================================
        // REAL BROWSER TEST 1: Close modal via real [X] button while save request in-flight
        // =========================================================================
        console.log("\n--- REAL BROWSER TEST 1: Close via real [X] button during in-flight save ---");

        // Prepare promise to capture the in-flight save request
        let serverSaveDeferred = null;
        const saveArrivedPromise = new Promise(resolve => {
            pendingSaveResolvers.push(({ res, body }) => {
                serverSaveDeferred = { res, body };
                resolve();
            });
        });

        // Click "Tạo bộ đề" button
        await evalInPage(`document.getElementById('btn-open-create-quiz').click()`);
        await new Promise(r => setTimeout(r, 600));

        // Verify modal has 'show' class (Bootstrap modal is visibly displayed)
        let isModalShown = await evalInPage(`document.getElementById('quizModal').classList.contains('show')`);
        assert.strictEqual(isModalShown, true, "Quiz modal must be shown via Bootstrap");

        // Fill form and check candidate question
        await evalInPage(`
            document.getElementById('quiz-title').value = 'Đề Test X Button';
            const chk = document.getElementById('chk-q-q_browser_01');
            if (chk) { chk.checked = true; chk.dispatchEvent(new Event('change')); }
        `);
        await new Promise(r => setTimeout(r, 200));

        // Click "Lưu bản nháp"
        await evalInPage(`document.getElementById('btn-quiz-save-draft').click()`);

        // Wait until server receives the POST request
        await saveArrivedPromise;
        console.log('[TEST] Server received POST /admin/quizzes. Modal A save is actively in-flight.');

        // User clicks the real [X] button (.btn-close) on the modal
        await evalInPage(`document.querySelector('#quizModal .btn-close').click()`);
        console.log('[TEST] Clicked real .btn-close [X] button in browser.');

        // Wait for Bootstrap transition (fade)
        await new Promise(r => setTimeout(r, 400));

        // Verify modal is hidden
        isModalShown = await evalInPage(`document.getElementById('quizModal').classList.contains('show')`);
        assert.strictEqual(isModalShown, false, "Modal must be hidden after clicking [X]");

        // Now let server resolve the delayed save request
        console.log('[TEST] Resolving delayed save request for Modal A...');
        serverSaveDeferred.res.writeHead(201, { 'Content-Type': 'application/json' });
        serverSaveDeferred.res.end(JSON.stringify({
            id: 'qz_late_x_01',
            title: 'Đề Test X Button',
            status: 'DRAFT'
        }));

        await new Promise(r => setTimeout(r, 300));

        // Verify that NO alert was fired for the closed modal
        let alerts = await evalInPage(`window.__recordedAlerts`);
        assert.strictEqual(alerts.length, 0, "No success alert must be fired for closed modal");

        // Now open Modal B
        await evalInPage(`document.getElementById('btn-open-create-quiz').click()`);
        await new Promise(r => setTimeout(r, 600));

        // Verify Modal B is open, pristine, with empty title
        isModalShown = await evalInPage(`document.getElementById('quizModal').classList.contains('show')`);
        assert.strictEqual(isModalShown, true, "Modal B must open cleanly");
        const modalBTitle = await evalInPage(`document.getElementById('quiz-title').value`);
        assert.strictEqual(modalBTitle, '', "Modal B title must be clean and empty");

        alerts = await evalInPage(`window.__recordedAlerts`);
        assert.strictEqual(alerts.length, 0, "Modal B must not receive any stale alert from Modal A");
        console.log("✅ REAL BROWSER TEST 1 PASSED: Close via real [X] button successfully isolates in-flight save.");

        // =========================================================================
        // REAL BROWSER TEST 2: Close modal via real Escape key while save request in-flight
        // =========================================================================
        console.log("\n--- REAL BROWSER TEST 2: Close via real Escape key during in-flight save ---");

        // Prepare promise to capture in-flight save request for Modal B
        let serverSaveDeferred2 = null;
        const saveArrivedPromise2 = new Promise(resolve => {
            pendingSaveResolvers.push(({ res, body }) => {
                serverSaveDeferred2 = { res, body };
                resolve();
            });
        });

        // Fill Modal B form
        await evalInPage(`
            document.getElementById('quiz-title').value = 'Đề Test Escape Key';
            const chk2 = document.getElementById('chk-q-q_browser_01');
            if (chk2) { chk2.checked = true; chk2.dispatchEvent(new Event('change')); }
        `);
        await new Promise(r => setTimeout(r, 200));

        // Click "Lưu bản nháp"
        await evalInPage(`document.getElementById('btn-quiz-save-draft').click()`);

        // Wait until server receives POST
        await saveArrivedPromise2;
        console.log('[TEST] Server received POST /admin/quizzes for Modal B.');

        // Dispatch real Escape key event via Chrome DevTools Protocol
        await cdp('Input.dispatchKeyEvent', { type: 'rawKeyDown', key: 'Escape', code: 'Escape', windowsVirtualKeyCode: 27 });
        await cdp('Input.dispatchKeyEvent', { type: 'keyUp', key: 'Escape', code: 'Escape', windowsVirtualKeyCode: 27 });
        console.log('[TEST] Dispatched real Escape key via CDP.');

        // Wait for Bootstrap transition
        await new Promise(r => setTimeout(r, 400));

        // Verify modal is hidden
        isModalShown = await evalInPage(`document.getElementById('quizModal').classList.contains('show')`);
        assert.strictEqual(isModalShown, false, "Modal must be hidden after pressing Escape");

        // Now let server resolve the delayed save request
        console.log('[TEST] Resolving delayed save request for Modal B...');
        serverSaveDeferred2.res.writeHead(201, { 'Content-Type': 'application/json' });
        serverSaveDeferred2.res.end(JSON.stringify({
            id: 'qz_late_esc_02',
            title: 'Đề Test Escape Key',
            status: 'DRAFT'
        }));

        await new Promise(r => setTimeout(r, 300));

        alerts = await evalInPage(`window.__recordedAlerts`);
        assert.strictEqual(alerts.length, 0, "No success alert must be fired for modal dismissed via Escape");

        // Open Modal C to verify system remains fully functional
        await evalInPage(`document.getElementById('btn-open-create-quiz').click()`);
        await new Promise(r => setTimeout(r, 600));

        isModalShown = await evalInPage(`document.getElementById('quizModal').classList.contains('show')`);
        assert.strictEqual(isModalShown, true, "Modal C must open cleanly");
        const modalCTitle = await evalInPage(`document.getElementById('quiz-title').value`);
        assert.strictEqual(modalCTitle, '', "Modal C title must be clean");

        console.log("✅ REAL BROWSER TEST 2 PASSED: Close via real Escape key successfully isolates in-flight save.");

        console.log("\nALL REAL BROWSER CDP TESTS PASSED SUCCESSFULLY!");
    } finally {
        if (pageWs) pageWs.close();
        if (browserWs) browserWs.close();
        chromeProc.kill('SIGKILL');
        server.close();
        console.log('[CLEANUP] Chrome process and test server stopped.');
    }
}

runBrowserTest().catch(err => {
    console.error('REAL BROWSER TEST FAILED:', err);
    process.exit(1);
});
