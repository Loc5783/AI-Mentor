import test from 'node:test';
import assert from 'node:assert/strict';
import {cleanTeamFiles} from './cleanup.mjs';

const response = (body, status = 200) => ({ok: status < 300, status, json: async () => body});

test('deletes files and job only after the project is gone', async () => {
  const calls = [];
  const fetcher = async (url, options) => {
    calls.push([url, options.method ?? 'GET']);
    if (url.includes('mentor_team_file_cleanup?select=')) return response([{project_id: 'p1', file_paths: ['p1/a/file.pdf']}]);
    if (url.includes('mentor_team_projects?')) return response([]);
    return response(null);
  };
  assert.deepEqual(await cleanTeamFiles('https://example.test', 'key', fetcher), {completed: 1, failed: 0});
  assert.deepEqual(calls.slice(1).map(([, method]) => method), ['GET', 'DELETE', 'DELETE']);
});

test('keeps job when storage deletion fails for a later retry', async () => {
  const calls = [];
  const fetcher = async (url, options) => {
    calls.push(url);
    if (url.includes('mentor_team_file_cleanup?select=')) return response([{project_id: 'p1', file_paths: ['p1/a/file.pdf']}]);
    if (url.includes('mentor_team_projects?')) return response([]);
    return response(null, 503);
  };
  assert.deepEqual(await cleanTeamFiles('https://example.test', 'key', fetcher), {completed: 0, failed: 1});
  assert.equal(calls.some(url => url.includes('mentor_team_file_cleanup?project_id=')), false);
});

test('does not delete files while project still exists', async () => {
  const calls = [];
  const fetcher = async url => {
    calls.push(url);
    if (url.includes('mentor_team_file_cleanup?select=')) return response([{project_id: 'p1', file_paths: ['p1/a/file.pdf']}]);
    return response([{id: 'p1'}]);
  };
  assert.deepEqual(await cleanTeamFiles('https://example.test', 'key', fetcher), {completed: 0, failed: 0});
  assert.equal(calls.length, 2);
});
