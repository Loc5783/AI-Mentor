import test from 'node:test';
import assert from 'node:assert/strict';
import { analyzeTeamProject, parseTeamWarnings } from './team_analysis.mjs';

const projectId = 'proj-123e4567-e89b-42d3-a456-426614174000';
const userId = '123e4567-e89b-42d3-a456-426614174001';
const requirements = [{ id: 'req-1', title: 'Tính đúng', description: 'Trình bày phép tính' }];
const documents = [{ id: 'doc-1', title: 'Bản thảo', content: 'Kết quả đo được là 25 mét mỗi giây.' }];
const response = (rows, ok = true, status = 200) => ({ ok, status, json: async () => rows });

test('TeamOS rejects nonmembers before loading documents, spending quota or calling AI', async () => {
  const calls = [];
  const result = await analyzeTeamProject({ base: 'https://example.invalid', headers: {}, userId, projectId,
    signal: undefined, fetcher: async url => { calls.push(url); return response([]); },
    pool: { request: () => { throw new Error('Provider must not be called'); } }, model: 'test' });
  assert.equal(result.status, 403);
  assert.equal(result.body.error_code, 'TEAM_MEMBERSHIP_REQUIRED');
  assert.equal(calls.length, 1);
  assert.match(calls[0], /mentor_team_members/);
});

test('TeamOS does not spend quota for a read-only viewer', async () => {
  let calls = 0;
  const result = await analyzeTeamProject({ base: 'https://example.invalid', headers: {}, userId, projectId,
    signal: undefined, fetcher: async () => { calls++; return response([{ user_id: userId, role: 'VIEWER' }]); },
    pool: { request: () => { throw new Error('Provider must not be called'); } }, model: 'test' });
  assert.equal(result.body.error_code, 'TEAM_EDIT_REQUIRED');
  assert.equal(calls, 1);
});

test('TeamOS validates evidence, stores a reviewable warning and deduplicates retries', async () => {
  const writes = [];
  const fetcher = async (url, init = {}) => {
    if (url.includes('mentor_team_members')) return response([{ user_id: userId, role: 'MEMBER' }]);
    if (url.includes('mentor_team_requirements')) return response(requirements);
    if (url.includes('mentor_team_documents')) return response(documents);
    if (url.includes('mentor_reserve_ai_request')) return response({});
    if (url.includes('mentor_team_warnings')) { writes.push(JSON.parse(init.body)); return response({}); }
    throw new Error(`Unexpected URL ${url}`);
  };
  const pool = { request: async () => response({ candidates: [{ content: { parts: [{ text: JSON.stringify({
    warnings: [{ type: 'CONTRADICTION', message: 'Cần kiểm tra số liệu đo', requirementId: 'req-1',
      docId: 'doc-1', excerpt: '25 mét mỗi giây', explanation: 'Đối chiếu với phép tính ở yêu cầu.' }],
  }) }] } }] }) };
  const args = { fetcher, base: 'https://example.invalid', headers: {}, userId, projectId,
    pool, model: 'test', signal: undefined };
  const first = await analyzeTeamProject(args);
  const second = await analyzeTeamProject(args);
  assert.equal(first.status, 200);
  assert.equal(second.status, 200);
  assert.equal(writes.length, 2);
  assert.equal(writes[0].id, writes[1].id);
  assert.equal(writes[0].status, 'NEEDS_CHECK');
  assert.equal(writes[0].created_by_ai, true);
  assert.equal(writes[0].excerpt, '25 mét mỗi giây');
});

test('TeamOS rejects invented source quotes and oversized results', () => {
  const invented = JSON.stringify({ warnings: [{ type: 'UNGROUNDED_CLAIM',
    message: 'Câu này thiếu nguồn', docId: 'doc-1', excerpt: 'một câu không tồn tại' }] });
  assert.throws(() => parseTeamWarnings(invented, requirements, documents), /INVALID_AI_OUTPUT/);
  assert.throws(() => parseTeamWarnings('{"warnings":[]}', requirements, documents, true), /INVALID_AI_OUTPUT/);
});
