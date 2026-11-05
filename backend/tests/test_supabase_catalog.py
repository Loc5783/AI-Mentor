from copy import deepcopy
import pytest
from fastapi import HTTPException
from aimentor.supabase_catalog import SupabaseCatalog, CatalogTransport


def snapshot():
    q = dict(id='q_original', code='TEST001', version=1, subject_id='physics', grade=10,
        chapter='I', topic='T', textbook='KNTT', prompt='Question?', option_a='1', option_b='2',
        option_c='3', option_d='4', correct_option='A', explanation='Answer', difficulty='EASY',
        points=10, image_url=None, source='Own', author='Teacher', license='Internal',
        status='DRAFT', created_by=None, created_at='2026-11-01T00:00:00+00:00',
        updated_at='2026-11-01T00:00:00+00:00')
    quiz = dict(id='quiz_original', title='Cloud quiz', subject_id='physics', grade=10,
        chapter='I', topic='T', difficulty='EASY', status='PUBLISHED',
        created_at='2026-11-01T00:00:00+00:00', questions=[dict(id='embedded_only', prompt='Original?',
            options=[dict(id=k, text=k) for k in 'ABC'], correctOption='A', explanation='Why', points=10)])
    return dict(questions=[q], quizzes=[quiz], exams=[])


class FakeTransport:
    def __init__(self):
        self.data = snapshot()
        self.version = 1
        self.cache = {}
        self.commits = 0
        self.conflict = False

    def rpc(self, **p):
        key = p.get('p_request_key')
        if key in self.cache:
            h, result = self.cache[key]
            if h != p['p_request_hash']:
                raise HTTPException(409, 'IDEMPOTENCY_CONFLICT')
            return dict(cached=True, response=deepcopy(result))
        if p['p_action'] == 'snapshot':
            return dict(data=deepcopy(self.data), token=str(self.version))
        if self.conflict or p['p_token'] != str(self.version):
            raise HTTPException(409, 'CATALOG_CHANGED_RELOAD')
        for table, rows in p['p_changes'].items():
            for row in rows:
                self.data[table] = [r for r in self.data[table] if r['id'] != row['id']] + [deepcopy(row)]
        self.version += 1
        self.commits += 1
        self.cache[key] = (p['p_request_hash'], deepcopy(p['p_response']))
        return dict(response=p['p_response'])


def test_reads_keep_embedded_questions_out_of_standalone_bank():
    t = FakeTransport()
    svc = SupabaseCatalog(t, 'library')
    assert len(svc.list_questions_admin()) == 1
    assert svc.get_question_by_id('embedded_only')['optionD'] == ''
    assert len(svc.list_quizzes_admin()) == 1
    assert t.commits == 0


def test_edit_uses_existing_validation_version_and_only_changed_rows():
    t = FakeTransport()
    original = deepcopy(t.data)
    svc = SupabaseCatalog(t, 'library')
    result = svc.create_or_update_question_admin(987, {'id': 'q_original', 'prompt': 'Updated?'}, expected_version=1)
    assert result['version'] == 2
    assert t.data['questions'][0]['created_by'] is None
    assert t.data['questions'][0]['created_at'] == original['questions'][0]['created_at']
    assert t.data['quizzes'] == original['quizzes']
    with pytest.raises(HTTPException) as err:
        svc.create_or_update_question_admin(987, {'id': 'q_original', 'prompt': 'Stale?'}, expected_version=1)
    assert err.value.status_code == 409
    assert t.commits == 1


def test_cloud_conflict_does_not_fall_back_to_local_storage():
    t = FakeTransport()
    t.conflict = True
    with pytest.raises(HTTPException) as err:
        SupabaseCatalog(t, 'library').publish_question_admin('q_original')
    assert err.value.status_code == 409
    assert t.data == snapshot()


def test_publishing_cloud_quiz_preserves_three_option_snapshot():
    t = FakeTransport()
    original = deepcopy(t.data['quizzes'][0]['questions'])
    SupabaseCatalog(t, 'library').publish_quiz_admin('quiz_original')
    assert t.data['quizzes'][0]['questions'] == original
    assert len(t.data['questions']) == 1


def test_import_is_draft_atomic_and_retry_safe_across_adapter_instances():
    t = FakeTransport()
    rows = [dict(code='NEW001', prompt='New?', optionA='a', optionB='b', optionC='c', optionD='d',
                 correctOption='B', subjectId='physics', grade=10)]
    svc = SupabaseCatalog(t, 'library')
    first = svc.confirm_import(987, rows, False, idempotency_key='upload-1')
    second = SupabaseCatalog(t, 'library').confirm_import(987, rows, False, idempotency_key='upload-1')
    assert first == second
    assert t.commits == 1
    assert len(t.data['questions']) == 2
    assert t.data['questions'][-1]['status'] == 'DRAFT'
    with pytest.raises(HTTPException) as err:
        svc.confirm_import(987, rows, True, idempotency_key='upload-1')
    assert err.value.status_code == 409
    with pytest.raises(HTTPException):
        svc.confirm_import(987, rows + [dict(rows[0], code='INVALID', optionD='')], False, idempotency_key='upload-2')
    assert t.commits == 1


def test_exam_cannot_publish_draft_question_and_can_create_draft():
    t = FakeTransport()
    svc = SupabaseCatalog(t, 'exam')
    payload = dict(title='Exam', subjectId='physics', grade=10, questionIds=['q_original'], status='PUBLISHED')
    with pytest.raises(HTTPException):
        svc.create_or_update_exam_admin(987, payload)
    assert t.commits == 0
    result = svc.create_or_update_exam_admin(987, dict(payload, status='DRAFT'))
    assert result['questionCount'] == 1
    assert t.data['exams'][0]['created_by'] is None
    assert len(svc.list_exams_admin()) == 1


def test_preview_checks_cloud_codes_and_does_not_write():
    t = FakeTransport()
    csv = b'code,prompt,optionA,optionB,optionC,optionD,correctOption\nTEST001,Question,a,b,c,d,A\n'
    result = SupabaseCatalog(t, 'library').parse_import_file(csv)
    assert result['errorCount'] == 1
    assert t.commits == 0


def test_new_quiz_without_chapter_uses_cloud_non_null_defaults():
    t = FakeTransport()
    SupabaseCatalog(t, 'library').create_or_update_quiz_admin(987, dict(
        title='New draft quiz', subjectId='physics', grade=10,
        questionIds=['q_original'], chapter=None, topic=None, status='DRAFT'))
    row = next(q for q in t.data['quizzes'] if q['id'] != 'quiz_original')
    assert isinstance(row['chapter'], str) and isinstance(row['topic'], str)
    assert len(row['questions'][0]['options']) == 4
    assert len(t.data['questions']) == 1


def test_new_cloud_question_keeps_verified_supabase_author_uuid():
    t = FakeTransport()
    actor = '1555b422-274d-40bb-a17a-3534b063923b'
    SupabaseCatalog(t, 'library').confirm_import(actor, [dict(code='AUTHOR001', prompt='New?',
        optionA='a', optionB='b', optionC='c', optionD='d', correctOption='A')], False,
        idempotency_key='author-import')
    assert t.data['questions'][-1]['created_by'] == actor


def test_transport_rejects_public_keys_and_insecure_origins():
    with pytest.raises(ValueError):
        CatalogTransport('https://test.supabase.co', 'sb_publishable_test')
    with pytest.raises(ValueError):
        CatalogTransport('http://test.supabase.co', 'sb_secret_test')


def test_transport_opaque_key_header_and_safe_connection_errors(monkeypatch):
    import httpx
    seen = []
    original = httpx.Client
    def handler(request):
        seen.append(request)
        return httpx.Response(503, json={'message': 'private upstream message'})
    monkeypatch.setattr(httpx, 'Client', lambda **kw: original(transport=httpx.MockTransport(handler), **kw))
    with pytest.raises(HTTPException) as err:
        CatalogTransport('https://test.supabase.co', 'sb_secret_test').rpc(p_action='snapshot')
    assert err.value.detail == {'code': 'SUPABASE_CATALOG_ERROR'}
    assert seen[0].headers['apikey'] == 'sb_secret_test'
    assert 'authorization' not in seen[0].headers


def test_admin_http_routes_use_cloud_and_enforce_local_roles(tmp_path, monkeypatch):
    from fastapi.testclient import TestClient
    from aimentor.config import Settings
    from aimentor.main import create_app
    import aimentor.supabase_catalog as module
    t = FakeTransport()
    monkeypatch.setattr(module, 'CatalogTransport', lambda *a: t)
    app = create_app(Settings(tmp_path, admin_catalog='supabase',
                              supabase_url='https://test.supabase.co', supabase_server_key='sb_secret_test'))
    with TestClient(app) as c:
        assert c.get('/admin/storage').status_code == 401
        session = c.post('/auth/sign-up', json=dict(displayName='Catalog Admin', email='catalog@test.example',
            password='Mentor01!2026', acceptedTermsVersion='2026-10')).json()
        h = {'Authorization': 'Bearer ' + session['accessToken']}
        assert c.get('/admin/questions', headers=h).status_code == 403
        with app.state.db.transaction() as db:
            db.execute("UPDATE users SET role='ADMIN', verified=1 WHERE email='catalog@test.example'")
        session = c.post('/auth/sign-in', json=dict(identifier='catalog@test.example', password='Mentor01!2026')).json()
        h = {'Authorization': 'Bearer ' + session['accessToken']}
        assert c.get('/admin/storage', headers=h).json()['catalog'] == 'supabase'
        assert c.get('/admin/questions', headers=h).json()[0]['id'] == 'q_original'
        response = c.post('/admin/questions', headers=h, json=dict(id='q_original', prompt='Via API?', expectedVersion=1))
        assert response.status_code == 200, response.text
        assert t.data['questions'][0]['prompt'] == 'Via API?'
        assert c.get('/admin/quizzes', headers=h).json()[0]['id'] == 'quiz_original'
        preview = c.post('/admin/import/preview', headers=h, content=(
            b'code,prompt,optionA,optionB,optionC,optionD,correctOption\nTEST001,Question,a,b,c,d,A\n'))
        assert preview.status_code == 200
        assert preview.json()['errorCount'] == 1
