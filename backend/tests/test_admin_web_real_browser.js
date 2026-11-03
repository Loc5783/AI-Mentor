const http = require('http');
const { spawn } = require('child_process');
const path = require('path');
const fs = require('fs');
const os = require('os');
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
let pendingQuestionSaveResolvers = [];

const questionsStore = [
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
        correctOption: 'B',
        version: 1
    },
    {
        id: 'q_browser_02',
        code: 'PHY10_02',
        prompt: 'Định luật I Newton còn gọi là gì?',
        subjectId: 'physics',
        grade: 10,
        difficulty: 'MEDIUM',
        status: 'DRAFT',
        optionA: 'Định luật quán tính',
        optionB: 'Định luật vạn vật hấp dẫn',
        optionC: 'Định luật bảo toàn cơ năng',
        optionD: 'Định luật bảo toàn động lượng',
        correctOption: 'A',
        version: 2
    }
];

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
        if (req.method === 'GET') {
            res.writeHead(200, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify(questionsStore));
            return;
        }

        if (req.method === 'POST') {
            let body = '';
            req.on('data', chunk => body += chunk);
            req.on('end', () => {
                const parsedBody = JSON.parse(body || '{}');
                console.log(`[SERVER] Received POST /admin/questions for ID: ${parsedBody.id}`);

                if (pendingQuestionSaveResolvers.length > 0) {
                    const resolver = pendingQuestionSaveResolvers.shift();
                    resolver({ res, body: parsedBody });
                } else {
                    const updated = {
                        ...parsedBody,
                        id: parsedBody.id || 'q_browser_01',
                        version: (parsedBody.version || 1) + 1
                    };
                    res.writeHead(200, { 'Content-Type': 'application/json' });
                    res.end(JSON.stringify(updated));
                }
            });
            return;
        }
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
    const profileDir = path.join(process.env.MENTOR_DATA_DIR ? path.dirname(process.env.MENTOR_DATA_DIR) : os.tmpdir(), 'chrome_real_test_profile');
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

        async function waitForModalClosed(modalId) {
            await new Promise(r => setTimeout(r, 800));
            const stillShown = await evalInPage(`document.getElementById('${modalId}').classList.contains('show')`);
            if (stillShown) {
                await new Promise(r => setTimeout(r, 400));
            }
        }

        await cdp('Page.enable');
        await cdp('Runtime.enable');

        pageWs.addEventListener('message', (msg) => {
            try {
                const parsed = JSON.parse(msg.data);
                if (parsed.method === 'Runtime.exceptionThrown') {
                    console.error('[BROWSER EXCEPTION]', JSON.stringify(parsed.params.exceptionDetails));
                }
            } catch(e) {}
        });

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

        // Switch to Questions tab
        await evalInPage(`showSection('questions')`);
        await new Promise(r => setTimeout(r, 600));

                // =========================================================================
        // REAL BROWSER TEST 3: Close questionEditModal via real [X] button while save request in-flight
        // =========================================================================
        console.log("\n--- REAL BROWSER TEST 3: Close question modal via real [X] button during in-flight save ---");

        let serverQSaveDeferred = null;
        const qSaveArrivedPromise = new Promise(resolve => {
            pendingQuestionSaveResolvers.push(({ res, body }) => {
                serverQSaveDeferred = { res, body };
                resolve();
            });
        });

        // Click ✏️ Sửa for Question 1
        await evalInPage(`
            const editBtns = Array.from(document.querySelectorAll('#questions-list button')).filter(b => b.textContent.includes('Sửa'));
            if (editBtns.length > 0) editBtns[0].click();
        `);
        await new Promise(r => setTimeout(r, 600));

        let isQModalShown = await evalInPage(`document.getElementById('questionEditModal').classList.contains('show')`);
        assert.strictEqual(isQModalShown, true, "Question Edit modal must be shown via Bootstrap");

        const qId1 = await evalInPage(`document.getElementById('q-edit-id').value`);
        assert.strictEqual(qId1, 'q_browser_01', "Question Edit modal must load q_browser_01");

        // Edit question prompt
        await evalInPage(`
            document.getElementById('q-edit-prompt').value = 'Chuyển động thẳng biến đổi đều có gia tốc thế nào? (Updated)';
            document.getElementById('btn-save-question').click();
        `);

        // Wait for server to receive POST /admin/questions
        await qSaveArrivedPromise;
        console.log('[TEST] Server received POST /admin/questions. Question 1 save is actively in-flight.');

        const btnQSaveDisabled = await evalInPage(`document.getElementById('btn-save-question').disabled`);
        assert.strictEqual(btnQSaveDisabled, true, "Save button must be disabled during in-flight save");

        // Click real [X] button (.btn-close) on questionEditModal
        await evalInPage(`document.querySelector('#questionEditModal .btn-close').click()`);
        console.log('[TEST] Clicked real .btn-close [X] button on questionEditModal.');

        // Wait for modal and backdrop to fully close
        await waitForModalClosed('questionEditModal');

        isQModalShown = await evalInPage(`document.getElementById('questionEditModal').classList.contains('show')`);
        assert.strictEqual(isQModalShown, false, "Question Edit modal must be hidden after clicking [X]");

        // Resolve delayed save request
        console.log('[TEST] Resolving delayed save request for Question 1...');
        serverQSaveDeferred.res.writeHead(200, { 'Content-Type': 'application/json' });
        serverQSaveDeferred.res.end(JSON.stringify({
            id: 'q_browser_01',
            code: 'PHY10_01',
            prompt: 'Chuyển động thẳng biến đổi đều có gia tốc thế nào? (Updated)',
            subjectId: 'physics',
            grade: 10,
            difficulty: 'EASY',
            status: 'PUBLISHED',
            optionA: 'Vận tốc không đổi',
            optionB: 'Gia tốc không đổi',
            optionC: 'Quãng đường không đổi',
            optionD: 'Không có gia tốc',
            correctOption: 'B',
            version: 2
        }));

        await new Promise(r => setTimeout(r, 300));

        // Verify NO alert fired
        alerts = await evalInPage(`window.__recordedAlerts`);
        assert.strictEqual(alerts.length, 0, "No success alert must be fired for closed question modal");

        // Open Question 2 modal
        const clickQ2Res = await evalInPage(`(() => {
            const editBtns = Array.from(document.querySelectorAll('#questions-list button')).filter(b => b.textContent.includes('Sửa'));
            if (editBtns.length <= 1) return 'NOT_ENOUGH_BTNS_' + editBtns.length;
            try {
                editBtns[1].click();
                return 'CLICKED_OK';
            } catch(e) {
                return 'CLICK_ERR: ' + e.message;
            }
        })()`);
        console.log('[TEST] Click Q2 button result:', clickQ2Res);
        await new Promise(r => setTimeout(r, 600));

        isQModalShown = await evalInPage(`document.getElementById('questionEditModal').classList.contains('show')`);
        assert.strictEqual(isQModalShown, true, "Question Edit modal for Question 2 must open cleanly");

        const qId2 = await evalInPage(`document.getElementById('q-edit-id').value`);
        assert.strictEqual(qId2, 'q_browser_02', "Question Edit modal must load q_browser_02");

        alerts = await evalInPage(`window.__recordedAlerts`);
        assert.strictEqual(alerts.length, 0, "Question 2 modal must not receive stale alert from Question 1");

        const btnQ2Ready = await evalInPage(`!document.getElementById('btn-save-question').disabled && document.getElementById('btn-save-question').textContent.includes('Lưu thay đổi')`);
        assert.strictEqual(btnQ2Ready, true, "Save button in Question 2 modal must be ready and enabled");

        // Close Question 2 cleanly
        await evalInPage(`document.querySelector('#questionEditModal .btn-close').click()`);
        await waitForModalClosed('questionEditModal');
        console.log("✅ REAL BROWSER TEST 3 PASSED: Close question edit modal via real [X] button successfully isolates in-flight save.");

        // =========================================================================
        // REAL BROWSER TEST 4: Close questionEditModal via real Escape key while save request in-flight
        // =========================================================================
        console.log("\n--- REAL BROWSER TEST 4: Close question modal via real Escape key during in-flight save ---");

        let serverQSaveDeferred2 = null;
        const qSaveArrivedPromise2 = new Promise(resolve => {
            pendingQuestionSaveResolvers.push(({ res, body }) => {
                serverQSaveDeferred2 = { res, body };
                resolve();
            });
        });

        // Reopen Question 1
        const clickT4Res = await evalInPage(`(() => {
            const editBtns = Array.from(document.querySelectorAll('#questions-list button')).filter(b => b.textContent.includes('Sửa'));
            if (editBtns.length === 0) return 'NO_BTNS';
            try {
                editBtns[0].click();
                return 'CLICKED_T4_OK_LEN_' + editBtns.length;
            } catch(e) {
                return 'CLICK_T4_ERR: ' + e.message;
            }
        })()`);
        console.log('[TEST 4] Click Question 1 result:', clickT4Res);
        await new Promise(r => setTimeout(r, 600));

        isQModalShown = await evalInPage(`document.getElementById('questionEditModal').classList.contains('show')`);
        assert.strictEqual(isQModalShown, true, "Question Edit modal must open");

        // Click save
        await evalInPage(`document.getElementById('btn-save-question').click()`);

        // Wait for server to receive POST /admin/questions
        await qSaveArrivedPromise2;
        console.log('[TEST] Server received POST /admin/questions for Escape test.');

        // Dispatch real Escape key via CDP
        await cdp('Input.dispatchKeyEvent', { type: 'rawKeyDown', key: 'Escape', code: 'Escape', windowsVirtualKeyCode: 27 });
        await cdp('Input.dispatchKeyEvent', { type: 'keyUp', key: 'Escape', code: 'Escape', windowsVirtualKeyCode: 27 });
        console.log('[TEST] Dispatched real Escape key via CDP on questionEditModal.');

        // Wait for modal and backdrop to fully close
        await waitForModalClosed('questionEditModal');

        isQModalShown = await evalInPage(`document.getElementById('questionEditModal').classList.contains('show')`);
        assert.strictEqual(isQModalShown, false, "Question Edit modal must be hidden after pressing Escape");

        // Resolve delayed save request
        console.log('[TEST] Resolving delayed save request for Question 1 (Escape test)...');
        serverQSaveDeferred2.res.writeHead(200, { 'Content-Type': 'application/json' });
        serverQSaveDeferred2.res.end(JSON.stringify({
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
            correctOption: 'B',
            version: 3
        }));

        await new Promise(r => setTimeout(r, 300));

        alerts = await evalInPage(`window.__recordedAlerts`);
        assert.strictEqual(alerts.length, 0, "No success alert must be fired for question modal dismissed via Escape");

        // Reopen Question 1 modal cleanly to verify system remains fully functional
        const preReopen = await evalInPage(`({
            openGen: typeof questionModalOpenGen !== 'undefined' ? questionModalOpenGen : 'undef',
            activeGen: typeof questionModalActiveGen !== 'undefined' ? questionModalActiveGen : 'undef',
            modalClass: document.getElementById('questionEditModal').className,
            backdrop: !!document.querySelector('.modal-backdrop'),
            btnCount: document.querySelectorAll('#questions-list button').length
        })`);
        console.log('[TEST 4 PRE-REOPEN]', JSON.stringify(preReopen));

        const clickReopenRes = await evalInPage(`(() => {
            const editBtns = Array.from(document.querySelectorAll('#questions-list button')).filter(b => b.textContent.includes('Sửa'));
            if (editBtns.length === 0) return 'NO_BTNS';
            try {
                editBtns[0].click();
                return 'CLICKED_REOPEN_OK';
            } catch(e) {
                return 'CLICK_ERR: ' + e.message;
            }
        })()`);
        console.log('[TEST 4 CLICK REOPEN RES]', clickReopenRes);

        await new Promise(r => setTimeout(r, 600));

        const postReopen = await evalInPage(`({
            openGen: typeof questionModalOpenGen !== 'undefined' ? questionModalOpenGen : 'undef',
            activeGen: typeof questionModalActiveGen !== 'undefined' ? questionModalActiveGen : 'undef',
            modalClass: document.getElementById('questionEditModal').className,
            isShown: document.getElementById('questionEditModal').classList.contains('show')
        })`);
        console.log('[TEST 4 POST-REOPEN]', JSON.stringify(postReopen));

        isQModalShown = postReopen.isShown;
        assert.strictEqual(isQModalShown, true, "Question Edit modal must reopen cleanly");

        const btnQReadyEsc = await evalInPage(`!document.getElementById('btn-save-question').disabled && document.getElementById('btn-save-question').textContent.includes('Lưu thay đổi')`);
        assert.strictEqual(btnQReadyEsc, true, "Save button must be ready and enabled");

        // Close cleanly
        await evalInPage(`document.querySelector('#questionEditModal .btn-close').click()`);
        await waitForModalClosed('questionEditModal');

        console.log("✅ REAL BROWSER TEST 4 PASSED: Close question edit modal via real Escape key successfully isolates in-flight save.");

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
