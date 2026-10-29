const fs = require('fs');
const path = require('path');
const assert = require('assert');

const adminWebPath = path.join(__dirname, '../aimentor/admin_web.py');
const pyContent = fs.readFileSync(adminWebPath, 'utf8');
const scriptMatch = pyContent.match(/<script>([\s\S]*?)<\/script>/);

let jsCode = scriptMatch[1]
    .replace('let token =', 'var token =')
    .replace('let questionModalOpenGen = 0;', 'var questionModalOpenGen = 0;')
    .replace('let questionModalActiveGen = 0;', 'var questionModalActiveGen = 0;')
    .replace('let questionModalActiveQid = null;', 'var questionModalActiveQid = null;')
    .replace('let activeQuestionSaveOwner = null;', 'var activeQuestionSaveOwner = null;');

function deferred() {
    let resolve, reject;
    const promise = new Promise((res, rej) => { resolve = res; reject = rej; });
    return { promise, resolve, reject };
}

let alertCalls = [];
let loadQuestionsCalls = 0;
let modalClosedCalls = 0;
let modalShownCalls = 0;

const qModalListeners = {};
const mockQuestionEditModal = {
    style: { display: 'none' },
    addEventListener: (ev, fn) => {
        qModalListeners[ev] = qModalListeners[ev] || [];
        qModalListeners[ev].push(fn);
    },
    dispatchEvent: (ev) => {
        const type = typeof ev === 'string' ? ev : ev.type;
        if (qModalListeners[type]) qModalListeners[type].slice().forEach(fn => fn(ev));
    }
};

const mockElements = {
    'questionEditModal': mockQuestionEditModal,
    'question-edit-modal-error': { style: { display: 'none' }, textContent: '' },
    'question-edit-modal-title': { textContent: '' },
    'q-edit-id': { value: '' },
    'q-edit-version': { value: '1' },
    'q-edit-status': { value: 'DRAFT' },
    'q-edit-code': { value: '' },
    'q-edit-subject': { value: 'physics' },
    'q-edit-grade': { value: '10' },
    'q-edit-chapter': { value: 'Chương 1' },
    'q-edit-topic': { value: 'Chủ đề 1' },
    'q-edit-textbook': { value: 'KNTT' },
    'q-edit-prompt': { value: 'Nội dung câu hỏi' },
    'q-edit-opt-a': { value: 'A' },
    'q-edit-opt-b': { value: 'B' },
    'q-edit-opt-c': { value: 'C' },
    'q-edit-opt-d': { value: 'D' },
    'q-edit-correct': { value: 'A' },
    'q-edit-difficulty': { value: 'EASY' },
    'q-edit-points': { value: '10' },
    'q-edit-explanation': { value: 'Lời giải' },
    'q-edit-source': { value: 'Sách' },
    'q-edit-author': { value: 'Tác giả' },
    'q-edit-license': { value: 'Nội bộ' },
    'btn-save-question': { disabled: false, textContent: '💾 Lưu thay đổi' }
};

global.window = global;
global.localStorage = {
    getItem: () => 'valid_token',
    setItem: () => {},
    removeItem: () => {}
};
global.alert = (msg) => { alertCalls.push(msg); };
global.document = {
    getElementById: (id) => mockElements[id] || { style: {}, value: '', textContent: '' },
    addEventListener: () => {}
};
global.bootstrap = {
    Modal: function(el) {
        this.el = el;
        this.show = () => { modalShownCalls++; this.el.style.display = 'block'; };
        this.hide = () => {
            modalClosedCalls++;
            if (this.el.dispatchEvent) {
                this.el.dispatchEvent({ type: 'hide.bs.modal' });
                this.el.dispatchEvent({ type: 'hidden.bs.modal' });
            }
            this.el.style.display = 'none';
        };
    }
};

let currentFetchHandler = null;
global.fetch = async (url, opts) => {
    if (currentFetchHandler) return currentFetchHandler(url, opts);
    return { ok: true, status: 200, json: async () => ({ id: 'Q1', version: 2 }) };
};

eval(jsCode);
global.token = 'test_token';
global.loadQuestions = async () => { loadQuestionsCalls++; };

async function verifyFixedCode() {
    console.log("--- RUNNING VERIFICATION ON FIXED CODE ---");

    // TEST SCENARIO A
    alertCalls = [];
    loadQuestionsCalls = 0;
    const fetchDefA = deferred();
    currentFetchHandler = () => fetchDefA.promise;

    const qA = { id: 'Q_A', code: 'QA_CODE', version: 1, prompt: 'Prompt A', optionA: '1', optionB: '2', optionC: '3', optionD: '4', correctOption: 'A' };
    openQuestionEditModal(qA);

    const savePromiseA = saveQuestionEdit();
    // Modal is closed while fetch is still pending
    closeQuestionEditModal();

    // Now fetch finishes
    fetchDefA.resolve({
        ok: true,
        status: 200,
        json: async () => ({ id: 'Q_A', code: 'QA_CODE', version: 2 })
    });
    await savePromiseA;

    console.log("Scenario A results with FIXED code:");
    console.log("- alertCalls count:", alertCalls.length, alertCalls);
    console.log("- loadQuestionsCalls:", loadQuestionsCalls);
    assert.strictEqual(alertCalls.length, 0, "Scenario A Failed: Alert was shown after modal closed!");
    assert.strictEqual(loadQuestionsCalls, 0, "Scenario A Failed: loadQuestions was called after modal closed!");
    console.log("-> Scenario A PASS: Stale response was completely discarded without alert or reload.");

    // TEST SCENARIO B
    alertCalls = [];
    loadQuestionsCalls = 0;
    const jsonDefA = deferred();
    let jsonCalledDef = deferred();

    currentFetchHandler = () => Promise.resolve({
        ok: true,
        status: 200,
        json: () => {
            jsonCalledDef.resolve();
            return jsonDefA.promise;
        }
    });

    openQuestionEditModal(qA);
    const savePromiseB = saveQuestionEdit();

    // Wait until code is actively inside await res.json()
    await jsonCalledDef.promise;

    // Now open modal for Q_B
    const qB = { id: 'Q_B', code: 'QB_CODE', version: 1, prompt: 'Prompt B', optionA: '1', optionB: '2', optionC: '3', optionD: '4', correctOption: 'B' };
    openQuestionEditModal(qB);
    const modalBGen = questionModalActiveGen;

    // Now res.json() of A completes
    jsonDefA.resolve({ id: 'Q_A', code: 'QA_CODE', version: 2 });
    await savePromiseB;

    console.log("\nScenario B results with FIXED code:");
    console.log("- alertCalls count:", alertCalls.length, alertCalls);
    console.log("- active modal gen after A finished:", questionModalActiveGen, "(was", modalBGen, "for B)");
    assert.strictEqual(questionModalActiveGen, modalBGen, "Scenario B Failed: Modal B was closed by A!");
    assert.strictEqual(questionModalActiveQid, 'Q_B', "Scenario B Failed: Active QID changed!");
    assert.strictEqual(alertCalls.length, 0, "Scenario B Failed: Alert for A was shown during modal B!");
    console.log("-> Scenario B PASS: Modal B remains open and active, A's response did not close B or alert.");

    console.log("\n>>> ALL SENSITIVITY VERIFICATION TESTS PASSED SUCCESSFULLY ON FIXED CODE!");
}

verifyFixedCode().catch(err => {
    console.error("Verification failed:", err);
    process.exit(1);
});
