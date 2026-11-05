const assert = require('assert');
const fs = require('fs');
const path = require('path');
const vm = require('vm');
const source = fs.readFileSync(path.join(__dirname, '../aimentor/admin_web.py'), 'utf8');
const start = source.indexOf('// One refresh at a time;');
const end = source.indexOf('let lastPreviewData = null;', start);
assert(start >= 0 && end > start);
const code = source.slice(start, end).replace(/let adminAuthGeneration/g, 'var adminAuthGeneration')
    .replace(/let adminRefreshFlight/g, 'var adminRefreshFlight');
function response(status, data = {}) { return {status, ok: status >= 200 && status < 300, json: async () => data}; }
function setup(fetch) {
    const saved = new Map();
    const context = vm.createContext({fetch, token: 'old-access', refreshToken: 'old-refresh',
        localStorage: {getItem: key => saved.get(key), setItem: (key, val) => saved.set(key, val)}, saved});
    vm.runInContext(code, context);
    return context;
}
async function run() {
    let refreshCalls = 0;
    let release;
    const gate = new Promise(resolve => { release = resolve; });
    const c = setup(async (url, opts) => {
        if (url === '/admin/auth/refresh') { ++refreshCalls; await gate; return response(200, {accessToken:'new-access', refreshToken:'new-refresh'}); }
        return response(opts.headers.Authorization === 'Bearer new-access' ? 200 : 401);
    });
    const a = c.adminFetch('/admin/questions');
    const b = c.adminFetch('/admin/quizzes');
    await new Promise(resolve => setImmediate(resolve));
    assert.strictEqual(refreshCalls, 1);
    release();
    assert((await a).ok && (await b).ok);
    assert.strictEqual(c.saved.get('admin_token'), 'new-access');
    assert.strictEqual(c.saved.get('admin_refresh'), 'new-refresh');

    let finish;
    const delayed = new Promise(resolve => { finish = resolve; });
    const d = setup(async url => {
        if (url === '/admin/auth/refresh') { await delayed; return response(200,{accessToken:'late',refreshToken:'late-refresh'}); }
        return response(401);
    });
    const pending = d.adminFetch('/admin/questions');
    await new Promise(resolve => setImmediate(resolve));
    d.adminAuthGeneration++;
    d.token = ''; d.refreshToken = '';
    finish();
    await assert.rejects(pending, /Phiên đăng nhập/);
    assert.strictEqual(d.token, '');
    assert.strictEqual(d.saved.size, 0);

    let requests = 0;
    const e = setup(async () => { requests++; return response(401); });
    assert.strictEqual((await e.adminFetch('/admin/auth/sign-in', {})).status, 401);
    assert.strictEqual(requests, 1);

    const f = setup(async url => url === '/admin/auth/refresh' ? response(403) : response(401));
    assert.strictEqual((await f.adminFetch('/admin/exams')).status,401);
    assert.strictEqual(f.token,'old-access');

    const g = setup(async url => url === '/admin/auth/refresh'
        ? response(200,{accessToken:'new-access',refreshToken:'new-refresh'}) : response(200));
    g.localStorage.setItem = () => { throw Error('Storage disabled'); };
    assert(await g.refreshAdminSession());
    assert.strictEqual(g.token,'new-access');
    console.log('Admin auth browser tests passed: single refresh, late logout, sign-in isolation, failed refresh, blocked storage.');
}
run().catch(error => { console.error(error); process.exitCode=1; });
