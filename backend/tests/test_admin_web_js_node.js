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
    .replace('let previewSequence = 0;', 'var previewSequence = 0;')
    .replace('let token =', 'var token =')
    .replace('let isImporting = false;', 'var isImporting = false;')
    .replace('let isPublishing = false;', 'var isPublishing = false;')
    .replace('let isSubmittingQuiz = false;', 'var isSubmittingQuiz = false;')
    .replace('let submittingModalGen = null;', 'var submittingModalGen = null;')
    .replace('let isPublishingQuiz = false;', 'var isPublishingQuiz = false;')
    .replace('let selectedQuizQuestionIds = new Set();', 'var selectedQuizQuestionIds = new Set();')
    .replace('let candidateQuestionsList = [];', 'var candidateQuestionsList = [];')
    .replace("let quizModalSubject = 'physics';", "var quizModalSubject = 'physics';")
    .replace("let quizModalGrade = '10';", "var quizModalGrade = '10';")
    .replace('let candidateLoadSeq = 0;', 'var candidateLoadSeq = 0;')
    .replace('let isCandidateLoading = false;', 'var isCandidateLoading = false;')
    .replace('let quizModalOpenGen = 0;', 'var quizModalOpenGen = 0;')
    .replace('let quizModalActiveGen = 0;', 'var quizModalActiveGen = 0;')
    .replace('let modalClosingGen = 0;', 'var modalClosingGen = 0;');

// Verify product code has Number.isSafeInteger and previewSequence
if (!jsCode.includes('Number.isSafeInteger(result.importedCount)')) {
    console.error("PRODUCT CODE CHECK FAILED: Number.isSafeInteger is missing in admin_web.py!");
    process.exit(1);
}
if (!jsCode.includes('previewSequence')) {
    console.error("PRODUCT CODE CHECK FAILED: previewSequence is missing in admin_web.py!");
    process.exit(1);
}

// 2. Setup Mock DOM & Environment
let alertCalls = [];
let showSectionCalls = [];
let confirmCalls = [];
let confirmResponse = true;
global.window = global;
global.window.confirm = (msg) => { confirmCalls.push(msg); return confirmResponse; };
global.confirm = global.window.confirm;
let mockFetchHandler = null;
let mockFetchResponse = null;
let mockFetchError = null;
    mockFetchResponse = null;
let fetchCalls = [];

const mockBtnDraft = { disabled: false };
const mockBtnPublish = { disabled: false };
const mockBtnPreview = { disabled: false };
const mockConfirmNotice = { style: { display: 'none' }, className: '', textContent: '', innerText: '' };
const mockImportFile = { files: [{ text: async () => "code,subject\nQ1,physics", arrayBuffer: async () => new ArrayBuffer(8) }], value: '' };
const mockImportSummary = { innerHTML: '' };
const mockImportErrors = { style: { display: 'none' } };
const mockErrorList = { innerHTML: '', appendChild: (el) => { mockErrorList.children.push(el); }, children: [] };
const mockTbody = { innerHTML: '', appendChild: (el) => { mockTbody.children.push(el); }, children: [] };
const mockImportResults = { style: { display: 'none' } };
const mockQuestionsList = { innerHTML: '', appendChild: () => {} };
const mockAuthAlert = { style: { display: 'none' }, innerText: '' };
const mockUserStatus = { innerText: '', className: '' };
const mockBtnLogin = { style: { display: '' } };
const mockBtnLogout = { style: { display: '' } };
const mockSectionQuestions = { style: { display: 'none' } };
const mockSectionDashboard = { style: { display: 'none' } };
const mockSectionImport = { style: { display: 'none' } };
const mockSectionQuizzes = { style: { display: 'none' } };
const mockQuizzesList = { innerHTML: '', children: [], appendChild: function(el) { this.children = this.children || []; this.children.push(el); } };
const mockFilterQuizSubject = { value: 'physics' };
const mockFilterQuizGrade = { value: '10' };
const mockFilterQuizStatus = { value: '' };
const mockQuizModalError = { style: { display: 'none' }, textContent: '' };
const mockQuizModalTitle = { textContent: '' };
const mockQuizId = { value: '' };
const mockQuizTitle = { value: '' };
const mockQuizSubject = { value: 'physics' };
const mockQuizGrade = { value: '10' };
const mockQuizDifficulty = { value: 'MEDIUM' };
const mockQuizChapter = { value: '' };
const mockQuizTopic = { value: '' };
const mockQuizSelectedCount = { textContent: '0' };
const mockQuizCandidateQuestions = { innerHTML: '', style: {}, children: [], appendChild: function(el) { this.children = this.children || []; this.children.push(el); } };
const mockBtnQuizSaveDraft = { disabled: false };
const mockBtnQuizSavePublish = { disabled: false };

const modalListeners = {};
const mockQuizModal = {
    style: { display: 'none' },
    addEventListener: function(ev, fn) {
        modalListeners[ev] = modalListeners[ev] || [];
        modalListeners[ev].push(fn);
    },
    removeEventListener: function(ev, fn) {
        if (modalListeners[ev]) {
            modalListeners[ev] = modalListeners[ev].filter(f => f !== fn);
        }
    },
    dispatchEvent: function(eventObj) {
        const type = typeof eventObj === 'string' ? eventObj : eventObj.type;
        if (modalListeners[type]) {
            modalListeners[type].slice().forEach(fn => fn(eventObj));
        }
    }
};

mockBtnDraft.parentNode = {
    insertBefore: (el, ref) => {}
};
mockBtnPublish.parentNode = mockBtnDraft.parentNode;

global.window = global;
global.localStorage = {
    _data: { admin_token: 'valid_token', admin_refresh: 'valid_refresh' },
    getItem: (k) => global.localStorage._data[k] || null,
    setItem: (k, v) => { global.localStorage._data[k] = String(v); },
    removeItem: (k) => { delete global.localStorage._data[k]; }
};

global.alert = (msg) => { alertCalls.push(msg); };
global.bootstrap = {
    Modal: function(el) {
        this.el = el;
        this.show = () => { if (this.el && this.el.style) this.el.style.display = 'block'; };
        this.hide = () => {
            if (this.el && this.el.dispatchEvent) {
                this.el.dispatchEvent({ type: 'hide.bs.modal' });
                this.el.dispatchEvent({ type: 'hidden.bs.modal' });
            }
            if (this.el && this.el.style) this.el.style.display = 'none';
        };
    }
};

const docListeners = {};
global.document = {
    addEventListener: (ev, fn) => {
        docListeners[ev] = docListeners[ev] || [];
        docListeners[ev].push(fn);
    },
    createElement: (tag) => ({
        tag,
        appendChild: function(c) { this.children = this.children || []; this.children.push(c); },
        textContent: '',
        className: '',
        innerHTML: '',
        style: {}
    }),
    getElementById: (id) => {
        if (id === 'btn-import-draft') return mockBtnDraft;
        if (id === 'btn-import-publish') return mockBtnPublish;
        if (id === 'btn-import-preview') return mockBtnPreview;
        if (id === 'import-confirm-notice') return mockConfirmNotice;
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
        if (id === 'section-quizzes') return mockSectionQuizzes;
        if (id === 'quizzes-list') return mockQuizzesList;
        if (id === 'filter-quiz-subject') return mockFilterQuizSubject;
        if (id === 'filter-quiz-grade') return mockFilterQuizGrade;
        if (id === 'filter-quiz-status') return mockFilterQuizStatus;
        if (id === 'quiz-modal-error') return mockQuizModalError;
        if (id === 'quiz-modal-title') return mockQuizModalTitle;
        if (id === 'quiz-id') return mockQuizId;
        if (id === 'quiz-title') return mockQuizTitle;
        if (id === 'quiz-subject') return mockQuizSubject;
        if (id === 'quiz-grade') return mockQuizGrade;
        if (id === 'quiz-difficulty') return mockQuizDifficulty;
        if (id === 'quiz-chapter') return mockQuizChapter;
        if (id === 'quiz-topic') return mockQuizTopic;
        if (id === 'quiz-selected-count') return mockQuizSelectedCount;
        if (id === 'quiz-candidate-questions') return mockQuizCandidateQuestions;
        if (id === 'btn-quiz-save-draft') return mockBtnQuizSaveDraft;
        if (id === 'btn-quiz-save-publish') return mockBtnQuizSavePublish;
        if (id === 'quizModal') return mockQuizModal;
        return { style: {}, value: '', innerHTML: '', innerText: '', textContent: '' };
    },
    querySelectorAll: () => []
};

global.fetch = async (url, opts) => {
    fetchCalls.push({ url, opts });
    if (mockFetchHandler) {
        const handled = mockFetchHandler(url, opts);
        if (handled !== undefined) return handled;
    }
    if (mockFetchError) throw mockFetchError;
    if (mockFetchResponse) return mockFetchResponse;
    return { ok: true, status: 200, json: async () => [] };
};

// Evaluate the actual product JavaScript code in global scope
eval(jsCode);

// Trigger actual product DOMContentLoaded callback
if (docListeners['DOMContentLoaded']) {
    docListeners['DOMContentLoaded'].forEach(fn => fn());
}

// Wrap showSection AFTER eval
const origShowSection = showSection;
showSection = (sec) => {
    showSectionCalls.push(sec);
    try { origShowSection(sec); } catch(e){}
};

const tick = () => new Promise(r => setImmediate(r));

async function resetState() {
    alertCalls = [];
    showSectionCalls = [];
    fetchCalls = [];
    mockFetchHandler = null;
    mockFetchError = null;
    mockFetchResponse = null;
    mockBtnDraft.disabled = false;
    mockBtnPublish.disabled = false;
    mockBtnPreview.disabled = false;
    mockSectionQuestions.style.display = 'none';
    mockImportResults.style.display = 'none';
    mockConfirmNotice.style.display = 'none';
    mockErrorList.children = [];
    mockTbody.children = [];
    mockImportFile.files = [{ arrayBuffer: async () => new ArrayBuffer(8) }];
    isImporting = false;
    isPublishing = false;
    mockQuizzesList.innerHTML = '';
    mockQuizzesList.children = [];
    mockQuizCandidateQuestions.innerHTML = '';
    mockQuizCandidateQuestions.children = [];
    mockQuizModalError.style.display = 'none';
    mockQuizModalError.textContent = '';
    mockBtnQuizSaveDraft.disabled = false;
    mockBtnQuizSavePublish.disabled = false;
    isSubmittingQuiz = false;
    submittingModalGen = null;
    isPublishingQuiz = false;
    selectedQuizQuestionIds.clear();
    candidateQuestionsList = [];
    confirmCalls = [];
    confirmResponse = true;
    quizModalSubject = 'physics';
    quizModalGrade = '10';
    mockQuizSubject.value = 'physics';
    mockQuizGrade.value = '10';
    candidateLoadSeq = 0;
    isCandidateLoading = false;
    quizModalOpenGen = 0;
    quizModalActiveGen = 0;
    modalClosingGen = 0;
    token = 'valid_token';
    previewSequence = 0;
    lastPreviewData = { validRows: [{ code: 'Q1', prompt: 'P1', subjectId: 'physics', grade: 10, correctOption: 'A', difficulty: 'EASY' }], validCount: 1, errorCount: 0, totalRows: 1 };
}

// Helper to create controllable promises for deterministic concurrency testing
function createControllablePromise() {
    let resolve, reject;
    const p = new Promise((res, rej) => {
        resolve = res;
        reject = rej;
    });
    p.resolve = resolve;
    p.reject = reject;
    return p;
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
    assert.strictEqual(showSectionCalls.includes("questions"), false);
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
    assert.strictEqual(mockBtnDraft.disabled, false);
    console.log("✅ Test 6 Passed: Network error handled and buttons restored.");

    // Test 7: SENSITIVITY PROOF (Old condition `typeof === 'number'` test)
    console.log("\n--- SENSITIVITY CHECK ON OLD CODE CONDITION ---");
    let oldCodeFailedAsExpected = false;
    const oldConditionTest = (importedCount) => (!lastPreviewData || typeof importedCount !== 'number' || importedCount < 0);
    if (oldConditionTest(1.5) === false) {
        oldCodeFailedAsExpected = true;
    }
    assert.strictEqual(oldCodeFailedAsExpected, true);
    console.log("✅ Sensitivity Proof Passed: Old condition `typeof === 'number'` accepted 1.5 (BUGGY), whereas new condition `Number.isSafeInteger` rejects 1.5 (FIXED).");

    // ==========================================
    // NEW TESTS: ISSUE 1 (PREVIEW RACE & INVALIDATION)
    // ==========================================
    console.log("\n--- RUNNING ISSUE 1 RACE & INVALIDATION TESTS ---");

    // Test 8: A starts before B; B arrives before A -> final preview MUST be B
    await resetState();
    const promiseFetchA = createControllablePromise();
    const promiseFetchB = createControllablePromise();

    let fetchCount = 0;
    mockFetchHandler = (url) => {
        fetchCount++;
        if (fetchCount === 1) return promiseFetchA;
        if (fetchCount === 2) return promiseFetchB;
    };

    mockImportFile.files = [{ arrayBuffer: async () => new ArrayBuffer(10) }];

    // Start Preview A
    const previewAPromise = handleImportPreview({ preventDefault: () => {} });
    assert.strictEqual(previewSequence, 1);
    assert.strictEqual(lastPreviewData, null, "lastPreviewData must be invalidated immediately before first await");
    await tick(); // Let A complete file read and enter fetch
    assert.strictEqual(fetchCount, 1, "A must have initiated fetch");

    // Start Preview B
    const previewBPromise = handleImportPreview({ preventDefault: () => {} });
    assert.strictEqual(previewSequence, 2);
    await tick(); // Let B complete file read and enter fetch
    assert.strictEqual(fetchCount, 2, "Both A and B must have initiated fetch");

    // Resolve B first
    promiseFetchB.resolve({
        ok: true,
        status: 200,
        json: async () => ({
            totalRows: 1,
            validCount: 1,
            errorCount: 0,
            validRows: [{ code: 'BATCH_B', prompt: 'Question B', subjectId: 'physics', grade: 10, correctOption: 'B', difficulty: 'EASY' }],
            errors: []
        })
    });
    await previewBPromise;
    assert.ok(lastPreviewData !== null, "B should populate preview data");
    assert.strictEqual(lastPreviewData.validRows[0].code, 'BATCH_B');

    // Resolve A later
    promiseFetchA.resolve({
        ok: true,
        status: 200,
        json: async () => ({
            totalRows: 1,
            validCount: 1,
            errorCount: 0,
            validRows: [{ code: 'BATCH_A', prompt: 'Question A', subjectId: 'physics', grade: 10, correctOption: 'A', difficulty: 'EASY' }],
            errors: []
        })
    });
    await previewAPromise;
    // CRITICAL: A must NOT overwrite B!
    assert.strictEqual(lastPreviewData.validRows[0].code, 'BATCH_B', "Outdated Preview A must NOT overwrite Preview B!");
    console.log("✅ Test 8 Passed: A starts before B; B finishes before A -> final preview remains B.");

    // Test 9: A errors after B succeeds -> B is NOT cleared or destroyed
    await resetState();
    const promiseA2 = createControllablePromise();
    const promiseB2 = createControllablePromise();
    let fetchCount2 = 0;
    mockFetchHandler = (url) => {
        fetchCount2++;
        if (fetchCount2 === 1) return promiseA2;
        if (fetchCount2 === 2) return promiseB2;
    };

    const pA2 = handleImportPreview({ preventDefault: () => {} });
    await tick();
    const pB2 = handleImportPreview({ preventDefault: () => {} });
    await tick();

    // B succeeds
    promiseB2.resolve({
        ok: true,
        status: 200,
        json: async () => ({
            totalRows: 1,
            validCount: 1,
            errorCount: 0,
            validRows: [{ code: 'STABLE_B', prompt: 'B', subjectId: 'physics', grade: 10, correctOption: 'A', difficulty: 'EASY' }],
            errors: []
        })
    });
    await pB2;
    assert.strictEqual(lastPreviewData.validRows[0].code, 'STABLE_B');

    // A fails with HTTP 500
    promiseA2.resolve({
        ok: false,
        status: 500,
        statusText: 'Internal Error',
        json: async () => ({ message: 'A crashed' })
    });
    await pA2;
    assert.ok(lastPreviewData !== null, "B must not be wiped out by A error");
    assert.strictEqual(lastPreviewData.validRows[0].code, 'STABLE_B');
    console.log("✅ Test 9 Passed: Outdated request A erroring does NOT wipe out successful Preview B.");

    // Test 10: File change during preview in-flight restores Preview button, isolates stale responses & handles full lifecycle
    await resetState();
    const promiseA10 = createControllablePromise();
    const promiseB10 = createControllablePromise();

    let fetchCount10 = 0;
    mockFetchHandler = (url) => {
        fetchCount10++;
        if (fetchCount10 === 1) return promiseA10;
        if (fetchCount10 === 2) return promiseB10;
    };

    mockImportFile.files = [{ arrayBuffer: async () => new ArrayBuffer(10) }];

    // 1. Start preview A, keep fetch A in-flight
    const pA10 = handleImportPreview({ preventDefault: () => {} });
    await tick();
    assert.strictEqual(fetchCount10, 1, "Fetch A must have been dispatched");

    // 2. Confirm Preview button is disabled while A is running
    assert.strictEqual(mockBtnPreview.disabled, true, "Preview button must be disabled while preview A is in flight");

    // 3. User switches to file B and real change event handler triggers
    mockImportFile.files = [{ arrayBuffer: async () => new ArrayBuffer(20) }];
    handleFileChange();

    // 4. Confirm immediately:
    //    - lastPreviewData = null
    //    - Draft / Publish buttons disabled
    //    - Preview button re-enabled for file B!
    assert.strictEqual(lastPreviewData, null, "lastPreviewData must be null on file change");
    assert.strictEqual(mockBtnDraft.disabled, true, "Draft button must be disabled on file change");
    assert.strictEqual(mockBtnPublish.disabled, true, "Publish button must be disabled on file change");
    assert.strictEqual(mockBtnPreview.disabled, false, "Preview button MUST be restored on file change to allow previewing new file B!");

    // 5. Start preview B
    const pB10 = handleImportPreview({ preventDefault: () => {} });
    await tick();
    assert.strictEqual(fetchCount10, 2, "Fetch B must have been dispatched");
    assert.strictEqual(mockBtnPreview.disabled, true, "Preview button disabled while B is in flight");

    // 6. Response A arrives while B is still waiting
    promiseA10.resolve({
        ok: true,
        status: 200,
        json: async () => ({
            totalRows: 1,
            validCount: 1,
            errorCount: 0,
            validRows: [{ code: 'STALE_A', prompt: 'Prompt A', subjectId: 'physics', grade: 10, correctOption: 'A', difficulty: 'EASY' }],
            errors: []
        })
    });
    await tick();

    // Verify A does NOT alter data and does NOT unlock preview/confirm contrary to B's in-flight state
    assert.strictEqual(lastPreviewData, null, "Response A must not populate lastPreviewData while B is pending");
    assert.strictEqual(mockBtnPreview.disabled, true, "Response A must not enable Preview button while B is in flight");
    assert.strictEqual(mockBtnDraft.disabled, true, "Response A must not enable Draft button while B is in flight");
    assert.strictEqual(mockBtnPublish.disabled, true, "Response A must not enable Publish button while B is in flight");

    // 7. Response B arrives successfully
    promiseB10.resolve({
        ok: true,
        status: 200,
        json: async () => ({
            totalRows: 1,
            validCount: 1,
            errorCount: 0,
            validRows: [{ code: 'VALID_B', prompt: 'Prompt B', subjectId: 'physics', grade: 10, correctOption: 'B', difficulty: 'EASY' }],
            errors: []
        })
    });
    await pB10;
    await pA10;

    // Verify final state: Batch B is displayed, Preview button restored, confirm enabled for valid B
    assert.ok(lastPreviewData !== null, "Batch B must populate preview data");
    assert.strictEqual(lastPreviewData.validRows[0].code, 'VALID_B', "Final preview must be batch B");
    assert.strictEqual(mockBtnPreview.disabled, false, "Preview button restored after B completes");
    assert.strictEqual(mockBtnDraft.disabled, false, "Draft button enabled for valid batch B");
    assert.strictEqual(mockBtnPublish.disabled, false, "Publish button enabled for valid batch B");
    console.log("✅ Test 10 Passed: File change re-enables Preview button, stale response A does not interfere with B.");

    // Test 10b: Response A returns error after file change but BEFORE sending B
    await resetState();
    const promiseA10b = createControllablePromise();
    mockFetchHandler = () => promiseA10b;

    mockImportFile.files = [{ arrayBuffer: async () => new ArrayBuffer(10) }];
    const pA10b = handleImportPreview({ preventDefault: () => {} });
    await tick();
    assert.strictEqual(mockBtnPreview.disabled, true);

    // User changes file before response A returns
    handleFileChange();
    assert.strictEqual(mockBtnPreview.disabled, false, "Preview button restored immediately upon file change");
    assert.strictEqual(lastPreviewData, null);

    // Response A fails with HTTP 500
    alertCalls = [];
    promiseA10b.resolve({
        ok: false,
        status: 500,
        statusText: 'Internal Error',
        json: async () => ({ message: 'Server crash on file A' })
    });
    await pA10b;
    await tick();

    // Stale error must be discarded: Preview still clickable, no data, no alert for old request
    assert.strictEqual(mockBtnPreview.disabled, false, "Preview button remains enabled after stale error");
    assert.strictEqual(lastPreviewData, null, "No stale data restored");
    assert.strictEqual(alertCalls.length, 0, "No error alert shown for discarded stale request");
    console.log("✅ Test 10b Passed: Outdated error does not disable Preview button or display stale alert.");

    // Test 10c: While confirming: calling change handler does NOT unlock Preview or change batch being written
    await resetState();
    const pendingConfirmPromise10c = createControllablePromise();
    mockFetchHandler = (url) => {
        if (url === '/admin/import/confirm') return pendingConfirmPromise10c;
        return { ok: true, status: 200, json: async () => [] };
    };

    lastPreviewData = {
        validRows: [{ code: 'IN_FLIGHT_CONFIRM_BATCH' }],
        validCount: 1,
        errorCount: 0
    };

    const confirmTask10c = confirmImport(true);
    await tick();

    assert.strictEqual(isImporting, true, "isImporting must be true during confirm");
    assert.strictEqual(mockBtnPreview.disabled, true, "Preview button must be disabled during confirm");
    assert.strictEqual(mockImportFile.disabled, true, "File input must be disabled during confirm");

    // Simulated user or DOM file change event during active confirmation
    handleFileChange();

    // Must remain locked!
    assert.strictEqual(mockBtnPreview.disabled, true, "Preview button MUST remain disabled if handleFileChange called during confirm");
    assert.strictEqual(isImporting, true, "isImporting must remain true");
    assert.ok(lastPreviewData !== null, "lastPreviewData must NOT be destroyed while confirm is in flight");

    // Complete confirmation
    pendingConfirmPromise10c.resolve({
        ok: true,
        status: 200,
        json: async () => ({ importedCount: 1 })
    });
    await confirmTask10c;
    await tick();

    assert.strictEqual(isImporting, false);
    assert.strictEqual(mockBtnPreview.disabled, false, "Preview button restored after confirm completes");
    console.log("✅ Test 10c Passed: File change during active confirm does not unlock Preview or disrupt batch.");

    // Test 11: Valid preview present; new file reading slowly -> confirm blocked IMMEDIATELY
    await resetState();
    lastPreviewData = { validRows: [{ code: 'VALID_OLD' }], validCount: 1, errorCount: 0 };
    mockBtnDraft.disabled = false;
    mockBtnPublish.disabled = false;

    const slowReadPromise = createControllablePromise();
    mockImportFile.files = [{ arrayBuffer: () => slowReadPromise }];

    // Trigger preview for slow file
    const slowPreviewPromise = handleImportPreview({ preventDefault: () => {} });

    // BEFORE arrayBuffer resolves:
    assert.strictEqual(lastPreviewData, null, "Must clear lastPreviewData BEFORE the first await!");
    assert.strictEqual(mockBtnDraft.disabled, true, "Must disable confirm buttons immediately");
    assert.strictEqual(mockBtnPublish.disabled, true);

    // If user tries to confirm while slow reading:
    alertCalls = [];
    fetchCalls = [];
    await confirmImport(true);
    assert.strictEqual(alertCalls.length, 1);
    assert.ok(alertCalls[0].includes("Không có câu hỏi hợp lệ nào để import!"));
    assert.strictEqual(fetchCalls.length, 0, "No confirm fetch allowed!");

    slowReadPromise.resolve(new ArrayBuffer(4));
    mockFetchResponse = {
        ok: true,
        status: 200,
        json: async () => ({ totalRows: 1, validCount: 1, errorCount: 0, validRows: [{ code: 'NEW' }], errors: [] })
    };
    await slowPreviewPromise;
    console.log("✅ Test 11 Passed: Slow file read invalidates old preview and blocks confirm immediately.");

    // Test 12: File read / fetch / JSON error -> no old batch left to import
    await resetState();
    lastPreviewData = { validRows: [{ code: 'PREV_BATCH' }], validCount: 1, errorCount: 0 };

    mockImportFile.files = [{ arrayBuffer: async () => { throw new Error("Disk read failure"); } }];
    alertCalls = [];
    await handleImportPreview({ preventDefault: () => {} });
    assert.strictEqual(lastPreviewData, null);
    assert.strictEqual(mockBtnDraft.disabled, true);
    assert.ok(alertCalls[0].includes("Không thể đọc file"));
    console.log("✅ Test 12 Passed: Read error leaves no old batch to import.");

    // Test 13: Double confirm click while request pending -> exactly 1 write request
    await resetState();
    const pendingConfirmPromise = createControllablePromise();
    mockFetchHandler = (url) => {
        if (url === '/admin/import/confirm') return pendingConfirmPromise;
    };

    const confirm1 = confirmImport(true);
    const confirm2 = confirmImport(true); // 2nd click while pending
    const confirm3 = confirmImport(true); // 3rd click while pending

    await tick();
    pendingConfirmPromise.resolve({
        ok: true,
        status: 200,
        json: async () => ({ importedCount: 1 })
    });

    await Promise.all([confirm1, confirm2, confirm3]);
    const confirmFetches = fetchCalls.filter(c => c.url === '/admin/import/confirm');
    assert.strictEqual(confirmFetches.length, 1, "Must only send 1 fetch for concurrent confirm clicks");
    console.log("✅ Test 13 Passed: Double/triple confirm clicks result in only 1 write request.");

    // Test 14: Preview operation during confirm -> rejected with warning
    await resetState();
    const longConfirmPromise = createControllablePromise();
    mockFetchHandler = (url) => {
        if (url === '/admin/import/confirm') return longConfirmPromise;
        return { ok: true, status: 200, json: async () => [] };
    };

    const cTask = confirmImport(true);
    await tick();
    assert.strictEqual(isImporting, true);

    alertCalls = [];
    await handleImportPreview({ preventDefault: () => {} });
    assert.ok(alertCalls.some(m => m.includes("Đang trong quá trình lưu câu hỏi")));

    longConfirmPromise.resolve({
        ok: true,
        status: 200,
        json: async () => ({ importedCount: 1 })
    });
    await cTask;
    console.log("✅ Test 14 Passed: Preview during confirm is locked/blocked with warning.");

    // ==========================================
    // NEW TESTS: ISSUE 3 (API ERROR PARSING & SAFE HTML)
    // ==========================================
    console.log("\n--- RUNNING ISSUE 3 ERROR PARSER & SAFE HTML TESTS ---");

    // Test 15: 400 with top-level message + row
    await resetState();
    mockFetchResponse = {
        ok: false,
        status: 400,
        json: async () => ({
            code: 'VALIDATION_FAILED',
            message: 'Nội dung câu hỏi không được để trống',
            row: 4,
            requestId: 'req-123'
        })
    };
    alertCalls = [];
    await confirmImport(true);
    assert.strictEqual(alertCalls.length, 1);
    assert.ok(alertCalls[0].includes('[Dòng 4]'));
    assert.ok(alertCalls[0].includes('Nội dung câu hỏi không được để trống'));
    assert.ok(alertCalls[0].includes('VALIDATION_FAILED'));
    assert.strictEqual(mockBtnDraft.disabled, false, "Buttons must be restored after error");
    assert.strictEqual(showSectionCalls.includes("questions"), false);
    console.log("✅ Test 15 Passed: 400 error displays row and message from API.");

    // Test 16: 409 Conflict
    await resetState();
    mockFetchResponse = {
        ok: false,
        status: 409,
        json: async () => ({
            code: 'CODE_CONFLICT',
            message: 'Mã câu hỏi đã tồn tại: PHY10_01',
            requestId: 'req-409'
        })
    };
    alertCalls = [];
    await confirmImport(true);
    assert.strictEqual(alertCalls.length, 1);
    assert.ok(alertCalls[0].includes('Mã câu hỏi đã tồn tại: PHY10_01'));
    console.log("✅ Test 16 Passed: 409 Code conflict displayed accurately.");

    // Test 17: 422 FastAPI validation detail array
    await resetState();
    mockFetchResponse = {
        ok: false,
        status: 422,
        json: async () => ({
            detail: [
                { loc: ['body', 'validRows'], msg: 'List should have at least 1 item' }
            ]
        })
    };
    alertCalls = [];
    await confirmImport(true);
    assert.strictEqual(alertCalls.length, 1);
    assert.ok(alertCalls[0].includes('validRows: List should have at least 1 item'));
    console.log("✅ Test 17 Passed: 422 FastAPI validation errors mapped properly.");

    // Test 18: 500 without JSON (e.g. HTML nginx error or crash)
    await resetState();
    mockFetchResponse = {
        ok: false,
        status: 500,
        statusText: 'Internal Server Error',
        json: async () => { throw new Error("Unexpected token < in JSON at position 0"); }
    };
    alertCalls = [];
    await confirmImport(true);
    assert.strictEqual(alertCalls.length, 1);
    assert.ok(alertCalls[0].includes('500'));
    console.log("✅ Test 18 Passed: Non-JSON 500 response displays human-readable status.");

    // Test 19: Message containing HTML is displayed safely (not executed)
    await resetState();
    const maliciousHtml = '<img src=x onerror=alert(1)> & <script>alert("xss")</script>';
    mockFetchResponse = {
        ok: true,
        status: 200,
        json: async () => ({
            totalRows: 1,
            validCount: 0,
            errorCount: 1,
            validRows: [],
            errors: [{ row: 1, message: maliciousHtml }]
        })
    };
    await handleImportPreview({ preventDefault: () => {} });
    assert.strictEqual(mockErrorList.children.length, 1);
    assert.strictEqual(mockErrorList.children[0].textContent, maliciousHtml, "Must use textContent, preventing XSS evaluation");
    console.log("✅ Test 19 Passed: Server error message with HTML tags safely rendered via textContent.");

    // --- RUNNING QUIZ MANAGEMENT UI JAVASCRIPT TESTS ---
    // Test 20: loadQuizzes renders quizzes list safely
    await resetState();
    mockFetchResponse = {
        ok: true,
        status: 200,
        json: async () => [
            {
                id: 'qz_01',
                title: 'Bộ đề Kiểm tra 1 tiết Lý 10 <script>',
                subjectId: 'physics',
                grade: 10,
                status: 'PUBLISHED',
                questionCount: 5,
                difficulty: 'MEDIUM',
                chapter: 'Chương 1',
                topic: 'Động học'
            }
        ]
    };
    await loadQuizzes();
    assert.strictEqual(mockQuizzesList.children.length, 1);
    assert.strictEqual(mockQuizzesList.children[0].children[1].textContent, 'Bộ đề Kiểm tra 1 tiết Lý 10 <script>');
    console.log("✅ Test 20 Passed: loadQuizzes fetches and renders quizzes safely without executing script tags.");

    // Test 21: openQuizModal resets fields for new quiz creation
    await resetState();
    mockFetchResponse = {
        ok: true,
        status: 200,
        json: async () => [
            { id: 'q_p1', code: 'PHY_01', prompt: 'Vận tốc là đại lượng gì?', status: 'PUBLISHED', difficulty: 'EASY' }
        ]
    };
    await openQuizModal(null);
    assert.strictEqual(mockQuizModalTitle.textContent, 'Tạo Bộ đề mới');
    assert.strictEqual(mockQuizTitle.value, '');
    assert.strictEqual(selectedQuizQuestionIds.size, 0);
    assert.strictEqual(mockQuizCandidateQuestions.children.length, 1);
    console.log("✅ Test 21 Passed: openQuizModal resets form and loads candidate questions for chosen subject/grade.");

    // Test 22: saveQuiz validates required fields (title, at least 1 question)
    await resetState();
    mockQuizTitle.value = '';
    await saveQuiz('DRAFT');
    assert.strictEqual(mockQuizModalError.style.display, 'block');
    assert.ok(mockQuizModalError.textContent.includes('tiêu đề'));

    mockQuizTitle.value = 'Bộ đề hợp lệ';
    selectedQuizQuestionIds.clear();
    await saveQuiz('DRAFT');
    assert.strictEqual(mockQuizModalError.style.display, 'block');
    assert.ok(mockQuizModalError.textContent.includes('ít nhất 1 câu hỏi'));
    console.log("✅ Test 22 Passed: saveQuiz client-side validation catches empty title and empty questions list.");

    // Test 23: saveQuiz handles server error (e.g. attempting to publish quiz containing draft questions)
    await resetState();
    mockQuizTitle.value = 'Bộ đề có câu hỏi nháp';
    selectedQuizQuestionIds.add('q_draft_01');
    candidateQuestionsList = [
        { id: 'q_draft_01', code: 'D01', prompt: 'Câu nháp', status: 'DRAFT' }
    ];
    mockFetchResponse = {
        ok: false,
        status: 400,
        json: async () => ({
            code: 'DRAFT_QUESTIONS_NOT_ALLOWED',
            message: 'Không thể xuất bản bộ đề chứa câu hỏi DRAFT',
            detail: 'Tất cả câu hỏi trong bộ đề xuất bản phải ở trạng thái PUBLISHED'
        })
    };
    await saveQuiz('PUBLISHED');
    assert.strictEqual(mockQuizModalError.style.display, 'block');
    assert.ok(mockQuizModalError.textContent.includes('Không thể xuất bản bộ đề chứa câu hỏi DRAFT'));
    console.log("✅ Test 23 Passed: saveQuiz displays API error message cleanly in modal when publishing violates integrity.");

    // Test 24: toggleQuizPublish calls publish API and prevents concurrent double-click
    await resetState();
    let publishCallCount = 0;
    fetchCalls = [];
    mockFetchHandler = async (url) => {
        if (url.includes('/publish')) publishCallCount++;
        await new Promise(r => setTimeout(r, 20));
        return {
            ok: true,
            status: 200,
            json: async () => ({ id: 'qz_01', status: 'DRAFT' })
        };
    };
    // Trigger double toggle concurrently
    const p1 = toggleQuizPublish('qz_01', false);
    const p2 = toggleQuizPublish('qz_01', false);
    await Promise.all([p1, p2]);
    assert.strictEqual(publishCallCount, 1, "Must prevent double click submission via isPublishingQuiz guard");
    console.log("✅ Test 24 Passed: toggleQuizPublish successfully calls publish API with anti-double-click protection.");

    // --- RUNNING TESTS FOR QUIZ CANDIDATE SELECTION & SUBJECT/GRADE INTEGRITY ---
    // Test 25 (Group A): Change subject/grade with user confirmation clears old selection, payload contains only new questions
    await resetState();
    // 1. User has opened modal and selected a Physics 10 question
    quizModalSubject = 'physics';
    quizModalGrade = '10';
    mockQuizSubject.value = 'physics';
    mockQuizGrade.value = '10';
    selectedQuizQuestionIds.add('q_phy_10');
    updateSelectedQuizCount();
    assert.strictEqual(selectedQuizQuestionIds.size, 1);
    assert.strictEqual(mockQuizSelectedCount.textContent, '1');

    // 2. User changes dropdowns to Math 12 and confirms
    mockQuizSubject.value = 'math';
    mockQuizGrade.value = '12';
    confirmResponse = true;
    mockFetchResponse = {
        ok: true,
        status: 200,
        json: async () => [
            { id: 'q_math_12', code: 'M12_01', prompt: 'Khảo sát hàm số', status: 'PUBLISHED', difficulty: 'MEDIUM' }
        ]
    };
    handleQuizSubjectGradeChange();
    await tick();

    // 3. Old selection must be cleared immediately, count reset to 0, active subject/grade updated
    assert.strictEqual(confirmCalls.length, 1, 'Confirmation prompt must be shown');
    assert.strictEqual(selectedQuizQuestionIds.size, 0, 'Old selection must be cleared on confirmed subject/grade change');
    assert.strictEqual(mockQuizSelectedCount.textContent, '0', 'Count display must be updated to 0');
    assert.strictEqual(quizModalSubject, 'math');
    assert.strictEqual(quizModalGrade, '12');

    // 4. Select the new Math 12 question
    toggleCandidateQuestion('q_math_12', true);
    assert.strictEqual(selectedQuizQuestionIds.size, 1);
    assert.ok(selectedQuizQuestionIds.has('q_math_12'));
    assert.ok(!selectedQuizQuestionIds.has('q_phy_10'), 'Old Physics 10 question must NOT exist in selection');

    // 5. Save quiz and check request payload
    mockQuizTitle.value = 'Bộ đề Toán 12';
    mockFetchResponse = {
        ok: true,
        status: 201,
        json: async () => ({ id: 'qz_m12', title: 'Bộ đề Toán 12', status: 'DRAFT' })
    };
    await saveQuiz('DRAFT');
    const saveFetch = fetchCalls.find(c => c.url === '/admin/quizzes' && c.opts && c.opts.method === 'POST');
    assert.ok(saveFetch, 'Save fetch request must be made');
    const savePayload = JSON.parse(saveFetch.opts.body);
    assert.strictEqual(savePayload.subjectId, 'math');
    assert.strictEqual(savePayload.grade, 12);
    assert.deepStrictEqual(savePayload.questionIds, ['q_math_12'], 'Payload must only contain Math 12 question, no leftover Physics 10');
    console.log("✅ Test 25 Passed (Group A): Changing subject/grade with confirm clears old selection; payload has only new questions.");

    // Test 26 (Group B): Cancel changing subject/grade restores dropdowns, keeps selection, sends no fetch
    await resetState();
    quizModalSubject = 'physics';
    quizModalGrade = '10';
    mockQuizSubject.value = 'physics';
    mockQuizGrade.value = '10';
    selectedQuizQuestionIds.add('q_phy_10');
    updateSelectedQuizCount();

    // User changes dropdown to chemistry 11 but clicks Cancel
    mockQuizSubject.value = 'chemistry';
    mockQuizGrade.value = '11';
    confirmResponse = false;
    const fetchCountBefore = fetchCalls.length;

    handleQuizSubjectGradeChange();
    await tick();

    // Dropdowns restored, selection kept, no fetch made
    assert.strictEqual(confirmCalls.length, 1);
    assert.strictEqual(mockQuizSubject.value, 'physics', 'Subject dropdown must revert to physics');
    assert.strictEqual(mockQuizGrade.value, '10', 'Grade dropdown must revert to 10');
    assert.strictEqual(quizModalSubject, 'physics');
    assert.strictEqual(quizModalGrade, '10');
    assert.strictEqual(selectedQuizQuestionIds.size, 1);
    assert.ok(selectedQuizQuestionIds.has('q_phy_10'), 'Selection must be preserved when change cancelled');
    assert.strictEqual(fetchCalls.length, fetchCountBefore, 'No network request should be sent when change is cancelled');
    console.log("✅ Test 26 Passed (Group B): Cancelling subject/grade change reverts dropdowns, preserves selection and sends no request.");

    // Test 27 (Group C): Reopening/editing quiz preserves selection; missing candidate triggers warning
    await resetState();
    const quizToEdit = {
        id: 'qz_edit_01',
        title: 'Bộ đề kiểm tra Lý',
        subjectId: 'physics',
        grade: 10,
        questionIds: ['q_phy_active', 'q_phy_missing_or_unpublished']
    };
    // Candidate list only returns q_phy_active (the other question is missing/unpublished)
    mockFetchResponse = {
        ok: true,
        status: 200,
        json: async () => [
            { id: 'q_phy_active', code: 'PHY_ACT', prompt: 'Chuyển động thẳng', status: 'PUBLISHED', difficulty: 'EASY' }
        ]
    };
    await openQuizModal(quizToEdit);

    // Selection preserved
    assert.strictEqual(selectedQuizQuestionIds.size, 2);
    assert.ok(selectedQuizQuestionIds.has('q_phy_active'));
    assert.ok(selectedQuizQuestionIds.has('q_phy_missing_or_unpublished'));

    // Check that warning for missing candidate is displayed in container
    assert.ok(mockQuizCandidateQuestions.children.length >= 2, 'Candidate container should render warning and candidate item');
    const warnEl = mockQuizCandidateQuestions.children[0];
    assert.ok(warnEl.children && warnEl.children.length >= 3, 'Warning box must contain header, missing ids, and remove button');
    assert.ok(warnEl.children[0].textContent.includes('không có trong danh sách'));
    assert.ok(warnEl.children[1].textContent.includes('q_phy_missing_or_unpublished'));

    // Attempting to save while missing question remains is blocked
    await saveQuiz('PUBLISHED');
    assert.strictEqual(mockQuizModalError.style.display, 'block');
    assert.ok(mockQuizModalError.textContent.includes('không có trong danh sách'));

    // User clicks the remove missing questions button
    const removeBtn = warnEl.children[2];
    assert.strictEqual(removeBtn.id, 'btn-remove-missing-questions');
    removeBtn.onclick();
    assert.strictEqual(selectedQuizQuestionIds.size, 1);
    assert.ok(selectedQuizQuestionIds.has('q_phy_active'));
    assert.ok(!selectedQuizQuestionIds.has('q_phy_missing_or_unpublished'));
    console.log("✅ Test 27 Passed (Group C): Edit preserves selection; missing candidates display warning and allow clean removal.");

    // Test 28 (Group D): Out-of-order race condition: Request A before B, B completes before A -> keeps B
    await resetState();
    const pA = createControllablePromise();
    const pB = createControllablePromise();

    let reqIndex = 0;
    mockFetchHandler = (url) => {
        reqIndex++;
        if (reqIndex === 1) return pA;
        if (reqIndex === 2) return pB;
    };

    // Start request A for Physics 10
    quizModalSubject = 'physics';
    quizModalGrade = '10';
    const loadPromiseA = loadCandidateQuestions();

    // Before A finishes, start request B for Chemistry 11
    quizModalSubject = 'chemistry';
    quizModalGrade = '11';
    const loadPromiseB = loadCandidateQuestions();

    // Resolve B FIRST with Chemistry data
    pB.resolve({
        ok: true,
        status: 200,
        json: async () => [
            { id: 'q_chem_01', code: 'CH_01', prompt: 'Phản ứng oxi hóa', status: 'PUBLISHED' }
        ]
    });
    await loadPromiseB;
    assert.strictEqual(candidateQuestionsList.length, 1);
    assert.strictEqual(candidateQuestionsList[0].id, 'q_chem_01');

    // Resolve A LATER with stale Physics data
    pA.resolve({
        ok: true,
        status: 200,
        json: async () => [
            { id: 'q_phy_stale', code: 'PHY_STALE', prompt: 'Stale Physics question', status: 'PUBLISHED' }
        ]
    });
    await loadPromiseA;

    // Must still hold Chemistry, stale Physics response A was discarded
    assert.strictEqual(candidateQuestionsList.length, 1);
    assert.strictEqual(candidateQuestionsList[0].id, 'q_chem_01', 'Stale request A must not overwrite newer request B');
    console.log("✅ Test 28 Passed (Group D): Out-of-order response (B before A) keeps B and discards stale A.");

    // Test 29 (Group D): Stale request A erroring does NOT wipe out successful request B
    await resetState();
    const pErrA = createControllablePromise();
    const pSuccessB = createControllablePromise();

    reqIndex = 0;
    mockFetchHandler = (url) => {
        reqIndex++;
        if (reqIndex === 1) return pErrA;
        if (reqIndex === 2) return pSuccessB;
    };

    // Trigger A then B
    quizModalSubject = 'physics';
    loadCandidateQuestions();
    quizModalSubject = 'math';
    loadCandidateQuestions();

    // B succeeds
    pSuccessB.resolve({
        ok: true,
        status: 200,
        json: async () => [{ id: 'q_math_new', code: 'M_NEW', prompt: 'Tích phân', status: 'PUBLISHED' }]
    });
    await tick();
    assert.strictEqual(candidateQuestionsList.length, 1);
    assert.strictEqual(candidateQuestionsList[0].id, 'q_math_new');

    // A errors afterwards
    pErrA.reject(new Error('Stale network failure'));
    await tick();

    // B's candidates must remain intact
    assert.strictEqual(candidateQuestionsList.length, 1);
    assert.strictEqual(candidateQuestionsList[0].id, 'q_math_new', 'Stale error must not wipe out successful candidates');
    console.log("✅ Test 29 Passed (Group D): Stale request erroring does not wipe out or show error for newer request.");

    // Test 30 (Group D): Closing/switching modal invalidates in-flight request from previous modal
    await resetState();
    const pModal1 = createControllablePromise();
    mockFetchHandler = () => pModal1;

    // Open modal 1
    openQuizModal({ id: 'qz_1', title: 'Đề 1', subjectId: 'physics', grade: 10 });
    const gen1 = quizModalOpenGen;

    // User closes modal 1 or opens modal 2
    closeQuizModal();
    assert.ok(quizModalOpenGen > gen1, 'Closing modal increments generation counter');

    // Stale fetch for modal 1 resolves
    pModal1.resolve({
        ok: true,
        status: 200,
        json: async () => [{ id: 'q_modal1_leak', code: 'LEAK' }]
    });
    await tick();

    // Should not render to candidate list
    assert.strictEqual(candidateQuestionsList.length, 0, 'In-flight response from closed modal must be ignored');
    console.log("✅ Test 30 Passed (Group D): Closing/switching modal invalidates previous modal requests.");

    // Test 31 (Group E): Save is blocked when candidate list is actively loading
    await resetState();
    isCandidateLoading = true;
    mockQuizTitle.value = 'Bộ đề hợp lệ';
    selectedQuizQuestionIds.add('q1');
    await saveQuiz('DRAFT');
    assert.strictEqual(mockQuizModalError.style.display, 'block');
    assert.ok(mockQuizModalError.textContent.includes('Đang tải danh sách câu hỏi'));
    console.log("✅ Test 31 Passed (Group E): Save is safely blocked while candidate list is actively loading.");

    // Test 32 (Group E): Save error preserves form and selection, does not show success
    await resetState();
    isCandidateLoading = false;
    mockQuizTitle.value = 'Bộ đề lưu thử';
    selectedQuizQuestionIds.add('q_test_save');
    candidateQuestionsList = [{ id: 'q_test_save', code: 'TS_01', prompt: 'P1', status: 'PUBLISHED' }];
    mockFetchResponse = {
        ok: false,
        status: 409,
        json: async () => ({ code: 'CONFLICT_TITLE', message: 'Tiêu đề bộ đề đã tồn tại' })
    };
    alertCalls = [];
    await saveQuiz('DRAFT');
    assert.strictEqual(mockQuizModalError.style.display, 'block');
    assert.ok(mockQuizModalError.textContent.includes('Tiêu đề bộ đề đã tồn tại'));
    assert.strictEqual(selectedQuizQuestionIds.has('q_test_save'), true, 'Selection must be preserved on save error');
    assert.strictEqual(mockQuizTitle.value, 'Bộ đề lưu thử', 'Form input must be preserved on save error');
    assert.strictEqual(alertCalls.length, 0, 'Success alert must NOT be fired when save fails');
    console.log("✅ Test 32 Passed (Group E): Save error preserves form and selection without closing or false success.");

    // --- RUNNING TESTS FOR ASYNC SAVE QUIZ GENERATION & ISOLATION SAFETY ---

    // Test 33 (Group A): Save Quiz A with slow res.json() body streaming does NOT close Modal B or show false success
    await resetState();
    // Open Modal A
    await openQuizModal(null);
    mockQuizTitle.value = 'Bộ đề A';
    selectedQuizQuestionIds.add('q_a1');
    candidateQuestionsList = [{ id: 'q_a1', code: 'A1', prompt: 'Câu A1', status: 'PUBLISHED' }];

    const pJsonA = createControllablePromise();
    mockFetchHandler = (url, opts) => {
        if (opts && (opts.method === 'POST' || opts.method === 'PUT')) {
            return {
                ok: true,
                status: 201,
                json: () => pJsonA
            };
        }
        return {
            ok: true,
            status: 200,
            json: async () => [{ id: 'q_b1', code: 'B1', prompt: 'Câu B1', status: 'PUBLISHED' }]
        };
    };
    alertCalls = [];

    // Trigger save on Modal A
    const savePromiseA = saveQuiz('DRAFT');
    await tick();

    // While res.json() of A is still pending, user closes Modal A and opens Modal B
    closeQuizModal();
    await openQuizModal({
        id: 'qz_b',
        title: 'Bộ đề B',
        subjectId: 'math',
        grade: 12,
        questionIds: ['q_b1']
    });
    mockQuizModal.style.display = 'block';
    assert.strictEqual(mockQuizTitle.value, 'Bộ đề B');
    assert.strictEqual(mockQuizModal.style.display, 'block');

    // Now resolve res.json() for Quiz A
    pJsonA.resolve({ id: 'qz_a_saved', title: 'Bộ đề A', status: 'DRAFT' });
    await savePromiseA;
    await tick();

    // Modal B MUST NOT be closed, form MUST NOT be mutated, alert for A MUST NOT be fired
    assert.strictEqual(mockQuizModal.style.display, 'block', 'Modal B must remain open');
    assert.strictEqual(mockQuizTitle.value, 'Bộ đề B', 'Modal B title must not be overwritten');
    assert.strictEqual(alertCalls.length, 0, 'Success alert for Quiz A must not be shown on Modal B');
    console.log("✅ Test 33 Passed (Group A): Slow res.json() body on Quiz A does not close Modal B or trigger false alert.");

    // Test 34 (Group B): Save Quiz A with slow error body does NOT leak error into Modal B
    await resetState();
    await openQuizModal(null);
    mockQuizTitle.value = 'Bộ đề A lỗi';
    selectedQuizQuestionIds.add('q_a2');
    candidateQuestionsList = [{ id: 'q_a2', code: 'A2', prompt: 'Câu A2', status: 'PUBLISHED' }];

    const pErrBodyA = createControllablePromise();
    mockFetchHandler = (url, opts) => {
        if (opts && (opts.method === 'POST' || opts.method === 'PUT')) {
            return {
                ok: false,
                status: 400,
                json: () => pErrBodyA
            };
        }
        return {
            ok: true,
            status: 200,
            json: async () => [{ id: 'q_b_clean', code: 'BC', prompt: 'Câu B clean', status: 'PUBLISHED' }]
        };
    };

    // Trigger save on Modal A
    const savePromiseErrA = saveQuiz('DRAFT');
    await tick();

    // While error body parsing is pending, user closes Modal A and opens Modal B
    closeQuizModal();
    await openQuizModal({
        id: 'qz_b_fresh',
        title: 'Bộ đề B sạch',
        subjectId: 'physics',
        grade: 10,
        questionIds: ['q_b_clean']
    });
    assert.strictEqual(mockQuizModalError.style.display, 'none');

    // Now error body of A finishes
    pErrBodyA.resolve({ message: 'Lỗi nghiệm thu riêng của đề A' });
    await savePromiseErrA;
    await tick();

    // Modal B must NOT display error from A
    assert.strictEqual(mockQuizModalError.style.display, 'none', 'Error from Quiz A must not appear in Modal B');
    assert.strictEqual(mockQuizTitle.value, 'Bộ đề B sạch');
    console.log("✅ Test 34 Passed (Group B): Slow error body on Quiz A does not leak error message into Modal B.");

    // Test 35 (Group C): finally of stale request does not prematurely unlock buttons of active loading modal
    await resetState();
    await openQuizModal(null);
    mockQuizTitle.value = 'Bộ đề A stale finally';
    selectedQuizQuestionIds.add('q_a3');
    candidateQuestionsList = [{ id: 'q_a3', code: 'A3', prompt: 'Câu A3', status: 'PUBLISHED' }];

    const pSaveFinallyA = createControllablePromise();
    const pCandidatesB = createControllablePromise();
    mockFetchHandler = (url, opts) => {
        if (opts && (opts.method === 'POST' || opts.method === 'PUT')) return pSaveFinallyA;
        if (url && url.includes('/admin/questions')) return pCandidatesB;
        return { ok: true, status: 200, json: async () => [] };
    };

    const savePromiseFinA = saveQuiz('DRAFT');
    await tick();

    // User closes Modal A and opens Modal B which is currently loading candidates
    closeQuizModal();
    const openModalBPromise = openQuizModal({ id: 'qz_b_loading', title: 'Đề B', subjectId: 'math', grade: 11, questionIds: [] });
    await tick();

    // Verify Modal B is actively loading and buttons are disabled
    assert.strictEqual(isCandidateLoading, true);
    assert.strictEqual(mockBtnQuizSaveDraft.disabled, true);
    assert.strictEqual(mockBtnQuizSavePublish.disabled, true);

    // Stale save of A finally rejects/resolves
    pSaveFinallyA.reject(new Error('Network drop on A'));
    await savePromiseFinA;
    await tick();

    // Modal B save buttons must STILL be disabled because Modal B is actively loading candidates
    assert.strictEqual(mockBtnQuizSaveDraft.disabled, true, 'Buttons must remain disabled while Modal B is loading');
    assert.strictEqual(mockBtnQuizSavePublish.disabled, true, 'Buttons must remain disabled while Modal B is loading');

    // When Modal B finishes loading candidates, buttons are enabled properly
    pCandidatesB.resolve({
        ok: true,
        status: 200,
        json: async () => [{ id: 'q_b_loaded', code: 'BL', prompt: 'Question B', status: 'PUBLISHED' }]
    });
    await openModalBPromise;
    await tick();

    assert.strictEqual(isCandidateLoading, false);
    assert.strictEqual(mockBtnQuizSaveDraft.disabled, false);
    assert.strictEqual(mockBtnQuizSavePublish.disabled, false);
    assert.strictEqual(isSubmittingQuiz, false, 'isSubmittingQuiz must not remain stuck');
    console.log("✅ Test 35 Passed (Group C): finally of stale request does not unlock buttons of actively loading modal.");

    const STANDARD_INVALID_MSG = 'Máy chủ trả kết quả không hợp lệ hoặc không khớp yêu cầu. Chưa xác nhận được trạng thái lưu; hãy kiểm tra danh sách bộ đề trước khi thử lại.';

    // Test 36 (Group D - Part A): Verify actual product DOMContentLoaded registered listeners on quizModal
    await resetState();
    assert.ok(modalListeners['hide.bs.modal'] && modalListeners['hide.bs.modal'].length > 0, 'Product DOMContentLoaded must register hide.bs.modal on quizModal');
    assert.ok(modalListeners['hidden.bs.modal'] && modalListeners['hidden.bs.modal'].length > 0, 'Product DOMContentLoaded must register hidden.bs.modal on quizModal');

    await openQuizModal({ id: 'qz_modal_bind', title: 'Đề test listener', subjectId: 'physics', grade: 10, questionIds: [] });
    const genBeforeDismiss = quizModalOpenGen;
    assert.strictEqual(quizModalActiveGen, quizModalOpenGen, 'Active gen must match open gen');

    // Dispatch event via the registered listener, NOT calling helper directly
    mockQuizModal.dispatchEvent({ type: 'hide.bs.modal' });
    assert.ok(quizModalOpenGen > genBeforeDismiss, 'Dispatching hide.bs.modal via registered listener must advance quizModalOpenGen');
    assert.strictEqual(quizModalActiveGen, 0, 'quizModalActiveGen must be reset to 0');
    assert.strictEqual(isSubmittingQuiz, false, 'isSubmittingQuiz must be reset');

    // Follow-up hidden.bs.modal does NOT double-increment
    const genAfterHide = quizModalOpenGen;
    mockQuizModal.dispatchEvent({ type: 'hidden.bs.modal' });
    assert.strictEqual(quizModalOpenGen, genAfterHide, 'hidden.bs.modal after hide.bs.modal must not double-increment generation');
    console.log("✅ Test 36 Passed (Part A): Actual product DOMContentLoaded registers listeners; dispatching hide.bs.modal advances gen and clears locks without double-increment.");

    // Test 36b (Group D - Part B): Fade window test: Save A starts -> hide.bs.modal dispatched -> response A resolves during fade -> no alert/leak -> open B -> hidden.bs.modal of A does not touch B
    await resetState();
    await openQuizModal({ id: 'qz_fade_a', title: 'Đề Fade A', subjectId: 'physics', grade: 10, questionIds: [] });
    mockQuizTitle.value = 'Đề Fade A';
    selectedQuizQuestionIds.add('q_fade_1');
    candidateQuestionsList = [{ id: 'q_fade_1', code: 'F1', prompt: 'P', status: 'PUBLISHED' }];

    const pFadeJsonA = createControllablePromise();
    mockFetchHandler = (url, opts) => {
        if (opts && (opts.method === 'POST' || opts.method === 'PUT')) {
            return {
                ok: true,
                status: 201,
                json: () => pFadeJsonA
            };
        }
        return { ok: true, status: 200, json: async () => [] };
    };
    alertCalls = [];

    const savePromiseFadeA = saveQuiz('DRAFT');
    await tick();

    // User initiates closing: Bootstrap fires hide.bs.modal (start of fade)
    mockQuizModal.dispatchEvent({ type: 'hide.bs.modal' });

    // While in the fade transition, response A arrives and resolves
    pFadeJsonA.resolve({ id: 'qz_fade_a', title: 'Đề Fade A', status: 'DRAFT' });
    await savePromiseFadeA;
    await tick();

    // Invalidation during fade MUST prevent success alert and form mutation
    assert.strictEqual(alertCalls.length, 0, 'No success alert must be displayed for response arriving during fade');

    // Now user opens Modal B while Modal A's fade is finishing
    await openQuizModal({ id: 'qz_fade_b', title: 'Đề B sau fade', subjectId: 'math', grade: 12, questionIds: [] });
    mockQuizTitle.value = 'Đề B sau fade';
    const genModalB = quizModalOpenGen;
    assert.strictEqual(quizModalActiveGen, genModalB);

    // Bootstrap completes Modal A's fade and fires hidden.bs.modal
    mockQuizModal.dispatchEvent({ type: 'hidden.bs.modal' });

    // Modal B MUST NOT be dismissed or modified by Modal A's hidden.bs.modal
    assert.strictEqual(quizModalOpenGen, genModalB, 'Late hidden.bs.modal of A must not increment generation of Modal B');
    assert.strictEqual(quizModalActiveGen, genModalB, 'Modal B must remain the active modal');
    assert.strictEqual(mockQuizTitle.value, 'Đề B sau fade', 'Modal B title must be preserved');
    console.log("✅ Test 36b Passed (Part B): Fade window invalidates response immediately; late hidden.bs.modal of A does not touch Modal B.");

    // Test 36c (Group D - Part B): closeQuizModal inline button click + hide.bs.modal does not double-dismiss
    await resetState();
    await openQuizModal({ id: 'qz_inline_close', title: 'Đề inline close', subjectId: 'physics', grade: 10, questionIds: [] });
    const genBeforeClose = quizModalOpenGen;

    // User clicks Close button: triggers closeQuizModal() AND Bootstrap's hide.bs.modal
    closeQuizModal();
    const genAfterFirst = quizModalOpenGen;
    assert.strictEqual(genAfterFirst, genBeforeClose + 1, 'Generation must increment exactly once on close');

    mockQuizModal.dispatchEvent({ type: 'hide.bs.modal' });
    assert.strictEqual(quizModalOpenGen, genAfterFirst, 'hide.bs.modal after closeQuizModal must not increment generation again');
    console.log("✅ Test 36c Passed (Part B): Inline close + Bootstrap event sequence does not double-dismiss or corrupt state.");

    // Test 37: Response missing valid object / missing ID
    await resetState();
    mockFetchHandler = (url, opts) => {
        if (opts && (opts.method === 'POST' || opts.method === 'PUT')) {
            return {
                ok: true,
                status: 200,
                json: async () => ({ title: 'Thiếu trường id hoàn toàn', status: 'DRAFT' })
            };
        }
        return { ok: true, status: 200, json: async () => [{ id: 'q_t37', code: 'T37', prompt: 'P', status: 'PUBLISHED' }] };
    };
    await openQuizModal(null);
    mockQuizTitle.value = 'Đề thiếu ID';
    selectedQuizQuestionIds.add('q_t37');
    candidateQuestionsList = [{ id: 'q_t37', code: 'T37', prompt: 'P', status: 'PUBLISHED' }];
    alertCalls = [];
    await saveQuiz('DRAFT');

    assert.strictEqual(mockQuizModalError.style.display, 'block');
    assert.strictEqual(mockQuizModalError.textContent, STANDARD_INVALID_MSG);
    assert.strictEqual(alertCalls.length, 0, 'No success alert on missing ID');
    assert.strictEqual(selectedQuizQuestionIds.has('q_t37'), true, 'Selection preserved');
    assert.strictEqual(mockQuizTitle.value, 'Đề thiếu ID', 'Form preserved');
    console.log("✅ Test 37 Passed: Response missing valid ID rejected with standard contract message; form & selection kept.");

    // Test 38: Response with whitespace-only ID
    await resetState();
    mockFetchHandler = (url, opts) => {
        if (opts && (opts.method === 'POST' || opts.method === 'PUT')) {
            return {
                ok: true,
                status: 200,
                json: async () => ({ id: '   ', title: 'ID toàn khoảng trắng', status: 'DRAFT' })
            };
        }
        return { ok: true, status: 200, json: async () => [{ id: 'q_t38', code: 'T38', prompt: 'P', status: 'PUBLISHED' }] };
    };
    await openQuizModal(null);
    mockQuizTitle.value = 'Đề ID khoảng trắng';
    selectedQuizQuestionIds.add('q_t38');
    candidateQuestionsList = [{ id: 'q_t38', code: 'T38', prompt: 'P', status: 'PUBLISHED' }];
    alertCalls = [];
    await saveQuiz('DRAFT');

    assert.strictEqual(mockQuizModalError.style.display, 'block');
    assert.strictEqual(mockQuizModalError.textContent, STANDARD_INVALID_MSG);
    assert.strictEqual(alertCalls.length, 0);
    assert.strictEqual(selectedQuizQuestionIds.has('q_t38'), true);
    console.log("✅ Test 38 Passed: Whitespace-only ID rejected; modal remains open with selection kept.");

    // Test 39: Response missing status entirely (reproduction of bug: user requested PUBLISHED, response has only id and title)
    await resetState();
    mockFetchHandler = (url, opts) => {
        if (opts && (opts.method === 'POST' || opts.method === 'PUT')) {
            return {
                ok: true,
                status: 201,
                json: async () => ({ id: 'quiz_a', title: 'A' })
            };
        }
        return { ok: true, status: 200, json: async () => [{ id: 'q_t39', code: 'T39', prompt: 'P', status: 'PUBLISHED' }] };
    };
    await openQuizModal(null);
    mockQuizTitle.value = 'Bộ đề A';
    selectedQuizQuestionIds.add('q_t39');
    candidateQuestionsList = [{ id: 'q_t39', code: 'T39', prompt: 'P', status: 'PUBLISHED' }];
    alertCalls = [];

    // User clicks PUBLISHED
    await saveQuiz('PUBLISHED');

    assert.strictEqual(mockQuizModalError.style.display, 'block');
    assert.strictEqual(mockQuizModalError.textContent, STANDARD_INVALID_MSG);
    assert.strictEqual(alertCalls.length, 0, 'Must NOT alert success or claim DRAFT when status is missing');
    assert.strictEqual(selectedQuizQuestionIds.has('q_t39'), true, 'Selection must be preserved');
    assert.strictEqual(mockQuizTitle.value, 'Bộ đề A', 'Form input must be preserved');
    console.log("✅ Test 39 Passed: Missing status in 201 response rejected with standard message; does not falsely claim success or DRAFT.");

    // Test 40: Response with status null or outside enum (e.g. ARCHIVED)
    await resetState();
    mockFetchHandler = (url, opts) => {
        if (opts && (opts.method === 'POST' || opts.method === 'PUT')) {
            return {
                ok: true,
                status: 200,
                json: async () => ({ id: 'quiz_archived', title: 'Đề lưu trữ', status: 'ARCHIVED' })
            };
        }
        return { ok: true, status: 200, json: async () => [{ id: 'q_t40', code: 'T40', prompt: 'P', status: 'PUBLISHED' }] };
    };
    await openQuizModal(null);
    mockQuizTitle.value = 'Đề ARCHIVED';
    selectedQuizQuestionIds.add('q_t40');
    candidateQuestionsList = [{ id: 'q_t40', code: 'T40', prompt: 'P', status: 'PUBLISHED' }];
    alertCalls = [];
    await saveQuiz('DRAFT');

    assert.strictEqual(mockQuizModalError.style.display, 'block');
    assert.strictEqual(mockQuizModalError.textContent, STANDARD_INVALID_MSG);
    assert.strictEqual(alertCalls.length, 0);
    console.log("✅ Test 40 Passed: Response with status outside DRAFT/PUBLISHED rejected with standard message.");

    // Test 41: TargetStatus mismatch: user requested PUBLISHED but API returned DRAFT
    await resetState();
    mockFetchHandler = (url, opts) => {
        if (opts && (opts.method === 'POST' || opts.method === 'PUT')) {
            return {
                ok: true,
                status: 200,
                json: async () => ({ id: 'quiz_mismatch_1', title: 'Đề', status: 'DRAFT' })
            };
        }
        return { ok: true, status: 200, json: async () => [{ id: 'q_t41', code: 'T41', prompt: 'P', status: 'PUBLISHED' }] };
    };
    await openQuizModal(null);
    mockQuizTitle.value = 'Đề yêu cầu PUBLISHED';
    selectedQuizQuestionIds.add('q_t41');
    candidateQuestionsList = [{ id: 'q_t41', code: 'T41', prompt: 'P', status: 'PUBLISHED' }];
    alertCalls = [];
    await saveQuiz('PUBLISHED');

    assert.strictEqual(mockQuizModalError.style.display, 'block');
    assert.strictEqual(mockQuizModalError.textContent, STANDARD_INVALID_MSG);
    assert.strictEqual(alertCalls.length, 0);
    assert.strictEqual(selectedQuizQuestionIds.has('q_t41'), true);
    console.log("✅ Test 41 Passed: Requesting PUBLISHED but receiving DRAFT is rejected as status mismatch.");

    // Test 42: TargetStatus mismatch: user requested DRAFT but API returned PUBLISHED
    await resetState();
    mockFetchHandler = (url, opts) => {
        if (opts && (opts.method === 'POST' || opts.method === 'PUT')) {
            return {
                ok: true,
                status: 200,
                json: async () => ({ id: 'quiz_mismatch_2', title: 'Đề', status: 'PUBLISHED' })
            };
        }
        return { ok: true, status: 200, json: async () => [{ id: 'q_t42', code: 'T42', prompt: 'P', status: 'PUBLISHED' }] };
    };
    await openQuizModal(null);
    mockQuizTitle.value = 'Đề yêu cầu DRAFT';
    selectedQuizQuestionIds.add('q_t42');
    candidateQuestionsList = [{ id: 'q_t42', code: 'T42', prompt: 'P', status: 'PUBLISHED' }];
    alertCalls = [];
    await saveQuiz('DRAFT');

    assert.strictEqual(mockQuizModalError.style.display, 'block');
    assert.strictEqual(mockQuizModalError.textContent, STANDARD_INVALID_MSG);
    assert.strictEqual(alertCalls.length, 0);
    console.log("✅ Test 42 Passed: Requesting DRAFT but receiving PUBLISHED is rejected as status mismatch.");

    // Test 43: ID mismatch when editing existing quiz: user edited quiz_edit_a, API returned quiz_edit_b
    await resetState();
    mockFetchHandler = (url, opts) => {
        if (opts && (opts.method === 'POST' || opts.method === 'PUT')) {
            return {
                ok: true,
                status: 200,
                json: async () => ({ id: 'quiz_edit_b', title: 'Đề sửa', status: 'DRAFT' })
            };
        }
        return { ok: true, status: 200, json: async () => [{ id: 'q_t43', code: 'T43', prompt: 'P', status: 'PUBLISHED' }] };
    };
    await openQuizModal({ id: 'quiz_edit_a', title: 'Đề sửa ban đầu', subjectId: 'physics', grade: 10, questionIds: ['q_t43'] });
    candidateQuestionsList = [{ id: 'q_t43', code: 'T43', prompt: 'P', status: 'PUBLISHED' }];
    alertCalls = [];
    await saveQuiz('DRAFT');

    assert.strictEqual(mockQuizModalError.style.display, 'block');
    assert.strictEqual(mockQuizModalError.textContent, STANDARD_INVALID_MSG);
    assert.strictEqual(alertCalls.length, 0);
    assert.strictEqual(selectedQuizQuestionIds.has('q_t43'), true);
    console.log("✅ Test 43 Passed: Editing quiz with ID mismatch in response rejected with standard message.");

    // Test 44: Successful save when editing with matching ID and matching PUBLISHED status
    await resetState();
    mockFetchHandler = (url, opts) => {
        if (opts && (opts.method === 'POST' || opts.method === 'PUT')) {
            return {
                ok: true,
                status: 200,
                json: async () => ({ id: 'quiz_edit_ok', title: 'Đề sửa chuẩn', status: 'PUBLISHED' })
            };
        }
        return { ok: true, status: 200, json: async () => [{ id: 'q_t44', code: 'T44', prompt: 'P', status: 'PUBLISHED' }] };
    };
    await openQuizModal({ id: 'quiz_edit_ok', title: 'Đề sửa chuẩn', subjectId: 'physics', grade: 10, questionIds: ['q_t44'] });
    candidateQuestionsList = [{ id: 'q_t44', code: 'T44', prompt: 'P', status: 'PUBLISHED' }];
    alertCalls = [];
    await saveQuiz('PUBLISHED');

    assert.strictEqual(alertCalls.length, 1);
    assert.ok(alertCalls[0].includes('Đã lưu thành công bộ đề: "Đề sửa chuẩn"! Trạng thái: Đã xuất bản'));
    assert.strictEqual(mockQuizModalError.style.display, 'none');
    console.log("✅ Test 44 Passed: Valid response with matched ID and PUBLISHED status succeeds and closes modal.");

    // Test 45: Successful save when creating new quiz with matched DRAFT status
    await resetState();
    mockFetchHandler = (url, opts) => {
        if (opts && (opts.method === 'POST' || opts.method === 'PUT')) {
            return {
                ok: true,
                status: 201,
                json: async () => ({ id: 'quiz_created_ok', title: 'Đề mới chuẩn', status: 'DRAFT' })
            };
        }
        return { ok: true, status: 200, json: async () => [{ id: 'q_t45', code: 'T45', prompt: 'P', status: 'PUBLISHED' }] };
    };
    await openQuizModal(null);
    mockQuizTitle.value = 'Đề mới chuẩn';
    selectedQuizQuestionIds.add('q_t45');
    candidateQuestionsList = [{ id: 'q_t45', code: 'T45', prompt: 'P', status: 'PUBLISHED' }];
    alertCalls = [];
    await saveQuiz('DRAFT');

    assert.strictEqual(alertCalls.length, 1);
    assert.ok(alertCalls[0].includes('Đã lưu thành công bộ đề: "Đề mới chuẩn"! Trạng thái: Bản nháp'));
    assert.strictEqual(mockQuizModalError.style.display, 'none');
    console.log("✅ Test 45 Passed: Valid response for new quiz with matched DRAFT status succeeds.");

    console.log("\nALL PRODUCT JAVASCRIPT TESTS PASSED SUCCESSFULLY!");
    console.log("ALL 47 JAVASCRIPT & DOM TESTS PASSED SUCCESSFULLY!");
}

runTests().catch(err => {
    console.error("TEST FAILED:", err);
    process.exit(1);
});
