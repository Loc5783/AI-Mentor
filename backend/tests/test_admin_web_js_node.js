const fs = require('fs');
const path = require('path');
const assert = require('assert');

// 1. Read admin_web.py from disk and extract actual script content
const adminWebPath = path.join(__dirname, '../aimentor/admin_web.py');
const pyContent = fs.readFileSync(adminWebPath, 'utf8');

const scriptMatch = pyContent.match(/<script>([\s\S]*?)<\/script>/);
if (!scriptMatch) {
    console.error("FAILED to extract <script> from admin_web.py");
    process.exit(1);
}

// Replace let with var for top-level variables so test can inspect/modify them
let jsCode = scriptMatch[1]
    .replace('let lastPreviewData = null;', 'var lastPreviewData = null;')
    .replace('let token =', 'var token =')
    .replace('let isImporting = false;', 'var isImporting = false;')
    .replace('let isPublishing = false;', 'var isPublishing = false;');

// Verify product code has Number.isSafeInteger
if (!jsCode.includes('Number.isSafeInteger(result.importedCount)')) {
    console.error("PRODUCT CODE CHECK FAILED: Number.isSafeInteger is missing in admin_web.py!");
    process.exit(1);
}

// 2. Setup Mock DOM & Environment
let alertCalls = [];
let showSectionCalls = [];
let mockFetchResponse = null;
let mockFetchError = null;

const mockBtnDraft = { disabled: false };
const mockBtnPublish = { disabled: false };
const mockImportFile = { files: [{ text: async () => "code,subject\nQ1,physics" }] };
const mockImportSummary = { innerHTML: '' };
const mockImportErrors = { style: { display: 'none' } };
const mockErrorList = { innerHTML: '', appendChild: () => {} };
const mockTbody = { innerHTML: '', appendChild: () => {} };
const mockImportResults = { style: { display: 'none' } };
const mockQuestionsList = { innerHTML: '', appendChild: () => {} };
const mockAuthAlert = { style: { display: 'none' }, innerText: '' };
const mockUserStatus = { innerText: '', className: '' };
const mockBtnLogin = { style: { display: '' } };
const mockBtnLogout = { style: { display: '' } };
const mockSectionQuestions = { style: { display: 'none' } };
const mockSectionDashboard = { style: { display: 'none' } };
const mockSectionImport = { style: { display: 'none' } };

global.window = global;
global.localStorage = {
    _data: { admin_token: 'valid_token', admin_refresh: 'valid_refresh' },
    getItem: (k) => global.localStorage._data[k] || null,
    setItem: (k, v) => { global.localStorage._data[k] = String(v); },
    removeItem: (k) => { delete global.localStorage._data[k]; }
};

global.alert = (msg) => { alertCalls.push(msg); };
global.bootstrap = { Modal: function() { return { show: () => {}, hide: () => {} }; } };

global.document = {
    addEventListener: () => {},
    createElement: (tag) => ({ appendChild: () => {}, textContent: '', className: '', innerHTML: '', style: {} }),
    getElementById: (id) => {
        if (id === 'btn-import-draft') return mockBtnDraft;
        if (id === 'btn-import-publish') return mockBtnPublish;
        if (id === 'import-file') return mockImportFile;
        if (id === 'import-summary') return mockImportSummary;
        if (id === 'import-errors') return mockImportErrors;
        if (id === 'error-list') return mockErrorList;
        if (id === 'preview-table-body') return mockTbody;
        if (id === 'import-results') return mockImportResults;
        if (id === 'questions-list') return mockQuestionsList;
        if (id === 'auth-alert') return mockAuthAlert;
        if (id === 'user-status') return mockUserStatus;
        if (id === 'btn-login') return mockBtnLogin;
        if (id === 'btn-logout') return mockBtnLogout;
        if (id === 'loginModal') return {};
        if (id === 'login-error') return { style: { display: 'none' } };
        if (id === 'login-identifier') return { value: 'admin' };
        if (id === 'login-password') return { value: 'pass' };
        if (id === 'section-questions') return mockSectionQuestions;
        if (id === 'section-dashboard') return mockSectionDashboard;
        if (id === 'section-import') return mockSectionImport;
        return { style: {}, value: '', innerHTML: '', innerText: '' };
    },
    querySelectorAll: () => []
};

global.fetch = async (url, opts) => {
    if (mockFetchError) throw mockFetchError;
    return mockFetchResponse;
};

// Evaluate the actual product JavaScript code in global scope
eval(jsCode);

// Wrap showSection AFTER eval
const origShowSection = showSection;
showSection = (sec) => {
    showSectionCalls.push(sec);
    try { origShowSection(sec); } catch(e){}
};

async function resetState() {
    alertCalls = [];
    showSectionCalls = [];
    mockFetchError = null;
    mockBtnDraft.disabled = false;
    mockBtnPublish.disabled = false;
    mockSectionQuestions.style.display = 'none';
    isImporting = false;
    isPublishing = false;
    token = 'valid_token';
    lastPreviewData = { validRows: [{ code: 'Q1', prompt: 'P1' }] };
}

async function runTests() {
    console.log("--- RUNNING PRODUCT JAVASCRIPT TESTS ---");

    // Test 1: Valid integer importedCount = 1 -> Success
    await resetState();
    mockFetchResponse = {
        ok: true,
        status: 200,
        json: async () => ({ importedCount: 1 })
    };
    await confirmImport(true);
    assert.strictEqual(alertCalls.length, 1);
    assert.ok(alertCalls[0].includes("Đã lưu thành công 1 câu hỏi!"));
    assert.strictEqual(showSectionCalls.includes("questions"), true);
    assert.strictEqual(mockBtnDraft.disabled, false);
    assert.strictEqual(mockBtnPublish.disabled, false);
    console.log("✅ Test 1 Passed: Valid importedCount = 1 allowed.");

    // Test 2: Float importedCount = 1.5 -> REJECTED (Number.isSafeInteger check)
    await resetState();
    mockFetchResponse = {
        ok: true,
        status: 200,
        json: async () => ({ importedCount: 1.5 })
    };
    await confirmImport(true);
    assert.strictEqual(alertCalls.length, 1);
    assert.ok(alertCalls[0].includes("Lỗi phản hồi: Kết quả import không hợp lệ."));
    assert.strictEqual(showSectionCalls.includes("questions"), false); // MUST NOT switch section
    assert.strictEqual(mockBtnDraft.disabled, false);
    assert.strictEqual(mockBtnPublish.disabled, false);
    console.log("✅ Test 2 Passed: Float importedCount = 1.5 REJECTED.");

    // Test 3: Invalid cases: -1, "1", null, NaN, Infinity -> REJECTED
    const invalidValues = [-1, "1", null, undefined, NaN, Infinity, Number.MAX_SAFE_INTEGER + 10];
    for (const val of invalidValues) {
        await resetState();
        mockFetchResponse = {
            ok: true,
            status: 200,
            json: async () => ({ importedCount: val })
        };
        await confirmImport(true);
        assert.strictEqual(alertCalls.length, 1, `Failed for val=${val}`);
        assert.ok(alertCalls[0].includes("Lỗi phản hồi"), `Failed for val=${val}`);
        assert.strictEqual(showSectionCalls.includes("questions"), false, `Failed for val=${val}`);
    }
    console.log("✅ Test 3 Passed: All invalid importedCount values REJECTED.");

    // Test 4: HTTP Status 401 -> Session expired
    await resetState();
    mockFetchResponse = { ok: false, status: 401, json: async () => ({ code: 'UNAUTHORIZED' }) };
    await confirmImport(true);
    assert.strictEqual(alertCalls.length, 1);
    assert.ok(alertCalls[0].includes("401"));
    assert.strictEqual(showSectionCalls.includes("questions"), false);
    console.log("✅ Test 4 Passed: HTTP 401 handled correctly.");

    // Test 5: HTTP Status 403 -> Forbidden
    await resetState();
    mockFetchResponse = { ok: false, status: 403, json: async () => ({ code: 'FORBIDDEN' }) };
    await confirmImport(true);
    assert.strictEqual(alertCalls.length, 1);
    assert.ok(alertCalls[0].includes("403"));
    assert.strictEqual(showSectionCalls.includes("questions"), false);
    console.log("✅ Test 5 Passed: HTTP 403 handled correctly.");

    // Test 6: Network Error / Timeout
    await resetState();
    mockFetchError = new Error("Network timeout");
    await confirmImport(true);
    assert.strictEqual(alertCalls.length, 1);
    assert.ok(alertCalls[0].includes("Lỗi kết nối mạng"));
    assert.strictEqual(showSectionCalls.includes("questions"), false);
    assert.strictEqual(mockBtnDraft.disabled, false); // Finally block executed
    console.log("✅ Test 6 Passed: Network error handled and buttons restored.");

    // Test 7: SENSITIVITY PROOF (Old condition `typeof === 'number'` test)
    console.log("\n--- SENSITIVITY CHECK ON OLD CODE CONDITION ---");
    let oldCodeFailedAsExpected = false;
    const oldConditionTest = (importedCount) => (!lastPreviewData || typeof importedCount !== 'number' || importedCount < 0);
    // On 1.5, oldConditionTest(1.5) returns false (which means old code considered 1.5 VALID!)
    if (oldConditionTest(1.5) === false) {
        oldCodeFailedAsExpected = true;
    }
    assert.strictEqual(oldCodeFailedAsExpected, true);
    console.log("✅ Sensitivity Proof Passed: Old condition `typeof === 'number'` accepted 1.5 (BUGGY), whereas new condition `Number.isSafeInteger` rejects 1.5 (FIXED).");

    console.log("\nALL PRODUCT JAVASCRIPT TESTS PASSED SUCCESSFULLY!");
}

runTests().catch(err => {
    console.error("TEST FAILED:", err);
    process.exit(1);
});
