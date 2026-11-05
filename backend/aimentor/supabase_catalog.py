"""Server catalog adapter; reuse existing domain rules in an isolated workspace.

The temporary SQLite database is a policy engine, never the persistence fallback.
Only changed catalog records are committed to Postgres in one guarded transaction.
Account data and learner progress are not copied into this workspace.
"""
from datetime import datetime, timezone
import hashlib
import json
from pathlib import Path
import tempfile
import uuid

import httpx
from fastapi import HTTPException

from .database import Database
from .library import LibraryService
from .exam import ExamService


def fingerprint(value):
    return hashlib.sha256(json.dumps(value, sort_keys=True, ensure_ascii=False,
                                    separators=(',', ':')).encode()).hexdigest()


def epoch(value):
    if isinstance(value, (float, int)):
        return value
    return datetime.fromisoformat(value.replace('Z', '+00:00')).timestamp()


def timestamp(value):
    return datetime.fromtimestamp(value, timezone.utc).isoformat()


class CatalogTransport:
    def __init__(self, url, key):
        from urllib.parse import urlparse
        parsed = urlparse(url)
        if (parsed.scheme != 'https' or not (parsed.hostname or '').endswith('.supabase.co')
                or parsed.path not in ('', '/') or parsed.query or parsed.fragment
                or parsed.username or parsed.port):
            raise ValueError('SUPABASE_URL must be a hosted Supabase HTTPS origin')
        if not (key.startswith('sb_secret_') or key.startswith('eyJ')):
            raise ValueError('A server secret key is required; publishable keys are not accepted')
        self.url = url.rstrip('/') + '/rest/v1/rpc/mentor_admin_catalog'
        self._headers = {'apikey': key}
        if key.startswith('eyJ'):
            self._headers['Authorization'] = 'Bearer ' + key

    def rpc(self, **payload):
        try:
            with httpx.Client(timeout=45, follow_redirects=False, trust_env=False) as client:
                response = client.post(self.url, headers=self._headers, json=payload)
        except httpx.HTTPError:
            raise HTTPException(503, detail={'code': 'SUPABASE_UNAVAILABLE'}) from None
        if response.is_error:
            try:
                message = response.json().get('message', '')
            except ValueError:
                message = ''
            if response.status_code == 409 or message in ('CATALOG_CHANGED_RELOAD', 'IDEMPOTENCY_CONFLICT'):
                raise HTTPException(409, detail={'code': message if message in (
                    'CATALOG_CHANGED_RELOAD', 'IDEMPOTENCY_CONFLICT') else 'CODE_CONFLICT'})
            raise HTTPException(503, detail={'code': 'SUPABASE_CATALOG_ERROR'})
        try:
            return response.json()
        except ValueError:
            raise HTTPException(503, detail={'code': 'SUPABASE_INVALID_RESPONSE'}) from None


TABLES = {'questions': 'questions', 'quizzes': 'library_quizzes', 'exams': 'exams'}
READS = {
    'list_questions_admin', 'get_question_by_id', 'list_quizzes_admin',
    'get_quiz_admin_by_id', 'generate_excel_template', 'parse_import_file',
    'list_exams_admin', 'get_exam_admin_by_id',
}
WRITES = {
    'create_or_update_question_admin', 'publish_question_admin',
    'create_or_update_quiz_admin', 'publish_quiz_admin', 'confirm_import',
    'create_or_update_exam_admin', 'publish_exam_admin',
}
ACTOR_METHODS = {name for name in WRITES if name.startswith('create_')} | {'confirm_import'}


def insert(db, table, row):
    columns = [r[1] for r in db.execute(f'PRAGMA table_info({table})')]
    row = {k: v for k, v in row.items() if k in columns}
    db.execute(f'INSERT INTO {table} ({",".join(row)}) VALUES ({",".join("?" for _ in row)})',
               list(row.values()))


def hydrate(database, snapshot):
    virtual_ids = set()
    with database.transaction() as db:
        # Synthetic actor is confined to the disposable policy workspace.
        insert(db, 'users', dict(id=1, username='catalog-policy', email='policy.invalid',
               password_hash='disabled', nickname='Policy', created_at=0, updated_at=0))
        ids = set()
        for name in ('questions', 'exams'):
            for source in snapshot[name]:
                row = dict(source, created_by=None)
                for field in ('created_at', 'updated_at'):
                    row[field] = epoch(row[field])
                if name == 'exams':
                    row['question_ids'] = json.dumps(row['question_ids'])
                insert(db, TABLES[name], row)
                if name == 'questions':
                    ids.add(row['id'])
        for source in snapshot['quizzes']:
            question_ids = []
            for q in source['questions']:
                qid = q['id']
                question_ids.append(qid)
                if qid in ids:
                    continue
                options = {str(o['id']).upper(): o['text'] for o in q['options']}
                row = dict(id=qid, code='CLOUD_' + fingerprint(qid)[:40], version=1,
                    subject_id=source['subject_id'], grade=source['grade'],
                    chapter=source['chapter'], topic=source['topic'], textbook='',
                    prompt=q['prompt'], correct_option=str(q.get('correctOption') or q.get('correctOptionId')).upper(),
                    explanation=q.get('explanation') or '', difficulty=q.get('difficulty') or source['difficulty'],
                    points=q.get('points', 10), status=source['status'], created_at=0, updated_at=0,
                    **{'option_' + key.lower(): options.get(key, '') for key in 'ABCD'})
                insert(db, 'questions', row)
                ids.add(qid)
                virtual_ids.add(qid)
            row = dict(source, question_ids=json.dumps(question_ids), created_at=epoch(source['created_at']))
            insert(db, 'library_quizzes', row)
    return virtual_ids


def read_rows(database):
    with database.read() as db:
        return {name: {r['id']: dict(r) for r in db.execute(f'SELECT * FROM {table}').fetchall()}
                for name, table in TABLES.items()}


def changes(before, after, snapshot, actor_id=None):
    result = {name: [] for name in TABLES}
    embedded = {q['id']: q for quiz in snapshot['quizzes'] for q in quiz['questions']}
    standalone = {q['id'] for q in snapshot['questions']}
    for name in TABLES:
        originals = {r['id']: r for r in snapshot[name]}
        for rid, local in after[name].items():
            if before[name].get(rid) == local:
                continue
            row = dict(local)
            original = originals.get(rid, {})
            if name == 'quizzes':
                row['chapter'] = row.get('chapter') or ''
                row['topic'] = row.get('topic') or ''
                qids = json.loads(row.pop('question_ids'))
                row['questions'] = []
                for qid in qids:
                    q = after['questions'][qid]
                    if qid not in standalone and before['questions'].get(qid) == q:
                        original_question = next((item for item in original.get('questions', [])
                                                  if item['id'] == qid), embedded[qid])
                        row['questions'].append(original_question)
                        continue
                    row['questions'].append(dict(id=qid, prompt=q['prompt'],
                        options=[dict(id=k, text=q['option_' + k.lower()]) for k in 'ABCD'],
                        correctOption=q['correct_option'], explanation=q['explanation'],
                        difficulty=q['difficulty'], points=q['points']))
            elif name == 'exams':
                row['question_ids'] = json.loads(row['question_ids'])
            for field in ('created_at', 'updated_at'):
                if field in row:
                    if field in original and before[name].get(rid, {}).get(field) == local[field]:
                        row[field] = original[field]
                    else:
                        row[field] = timestamp(row[field])
            if 'created_by' in row:
                # Local administrator ids are not Supabase Auth UUIDs.
                row['created_by'] = original.get('created_by', actor_id)
            result[name].append(row)
    return result


class SupabaseCatalog:
    def __init__(self, transport, kind):
        self.transport = transport
        self.kind = kind

    def __getattr__(self, method):
        allowed = READS | WRITES
        if method not in allowed:
            raise AttributeError(method)

        def call(*args, **kwargs):
            writing = method in WRITES
            request_key = request_hash = None
            if writing:
                request_hash = fingerprint([method, args, kwargs])
                if method == 'confirm_import':
                    key = kwargs.get('idempotency_key') or (args[3] if len(args) > 3 else None)
                    if key and (not isinstance(key, str) or len(key.strip()) > 128):
                        raise HTTPException(400, detail={'code': 'INVALID_IDEMPOTENCY_KEY'})
                    request_key = fingerprint([method, str(args[0]), (key or '').strip() or request_hash])
                else:
                    request_key = uuid.uuid4().hex
            loaded = self.transport.rpc(p_action='snapshot', p_request_key=request_key,
                                        p_request_hash=request_hash)
            if loaded.get('cached'):
                return loaded['response']
            snapshot = loaded['data']
            with tempfile.TemporaryDirectory(prefix='mentor-catalog-') as folder:
                database = Database(Path(folder))
                database.initialize()
                virtual_ids = hydrate(database, snapshot)
                before = read_rows(database) if writing else None
                service = LibraryService(database) if self.kind == 'library' else ExamService(database)
                local_args = list(args)
                if method in ACTOR_METHODS:
                    local_args[0] = 1
                result = getattr(service, method)(*local_args, **kwargs)
                if method == 'list_questions_admin':
                    result = [r for r in result if r['id'] not in virtual_ids]
                if writing:
                    actor_id = None
                    if method in ACTOR_METHODS:
                        try:
                            actor_id = str(uuid.UUID(str(args[0])))
                        except (ValueError, TypeError):
                            pass
                    delta = changes(before, read_rows(database), snapshot, actor_id)
                    saved = self.transport.rpc(p_action='commit', p_token=loaded['token'],
                        p_changes=delta, p_request_key=request_key,
                        p_request_hash=request_hash, p_response=result)
                    return saved['response']
                return result
        return call
