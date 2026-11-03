"""Regression tests for answer secrecy, replay and bounded provider work."""
import threading
from concurrent.futures import ThreadPoolExecutor

import pytest
from fastapi import HTTPException
from fastapi.testclient import TestClient

from aimentor.ai_practice import AiPracticeService, FakeAiProvider
from aimentor.config import Settings
from aimentor.main import create_app


@pytest.fixture
def service(tmp_path, monkeypatch):
    monkeypatch.setenv('AI_PROVIDER_TYPE', 'unconfigured')
    app = create_app(Settings(tmp_path))
    with TestClient(app) as client:
        user = client.post('/auth/sign-up', json={
            'displayName': 'Regression', 'email': 'regression@school.test',
            'password': 'Password123!', 'acceptedTermsVersion': '2026-10',
        }).json()
        instance = app.state.ai_practice
        instance.set_provider(FakeAiProvider())
        yield instance, int(user['userId'])


def start(service, key='create-1'):
    s, user = service
    return s.create_session(user, 'physics', 10, 'Motion', 'EASY', 1, key)


def test_explanation_cannot_reveal_unanswered_question(service):
    s, user = service
    session = start(service)
    qid = session['questions'][0]['id']
    with pytest.raises(HTTPException) as error:
        s.explain_more(user, session['id'], qid, 'Show the solution')
    assert error.value.detail['code'] == 'ANSWER_REQUIRED'
    s.submit_answer(user, session['id'], qid, 'B', 'answer-1')
    assert s.explain_more(user, session['id'], qid, 'Explain')['explanation']


def test_answer_retry_preserves_first_try_even_after_later_answer(service):
    s, user = service
    session = start(service)
    qid = session['questions'][0]['id']
    first = s.submit_answer(user, session['id'], qid, 'A', 'attempt-1')
    assert first['firstTryCorrect']
    assert s.submit_answer(user, session['id'], qid, 'A', 'attempt-1') == first
    s.submit_answer(user, session['id'], qid, 'B', 'attempt-2')
    # A delayed replay returns its original result, without overwriting newer state.
    assert s.submit_answer(user, session['id'], qid, 'A', 'attempt-1') == first
    current = s.get_session(user, session['id'])['answers'][qid]
    assert current['selectedOption'] == 'B'
    assert current['firstTryCorrect']
    assert current['attemptsCount'] == 2
    with pytest.raises(HTTPException) as error:
        s.submit_answer(user, session['id'], qid, 'D', 'attempt-1')
    assert error.value.status_code == 409


def test_wrong_first_try_does_not_improve_when_retried(service):
    s, user = service
    session = start(service)
    qid = session['questions'][0]['id']
    s.submit_answer(user, session['id'], qid, 'B', 'wrong')
    assert not s.submit_answer(user, session['id'], qid, 'A', 'retry')['firstTryCorrect']


def test_legacy_identical_answer_replay_is_safe(service):
    s, user = service
    session = start(service)
    qid = session['questions'][0]['id']
    first = s.submit_answer(user, session['id'], qid, 'A')
    assert s.submit_answer(user, session['id'], qid, 'A') == first


def test_concurrent_creation_claim_is_shared_across_service_instances(service):
    s, user = service
    entered, release = threading.Event(), threading.Event()

    class BlockingProvider(FakeAiProvider):
        calls = 0

        def generate_questions(self, *args, **kwargs):
            self.calls += 1
            entered.set()
            assert release.wait(5)
            return super().generate_questions(*args, **kwargs)

    provider = BlockingProvider()
    s.set_provider(provider)
    other = AiPracticeService(s.db, provider)
    with ThreadPoolExecutor(max_workers=1) as pool:
        pending = pool.submit(start, service)
        assert entered.wait(5)
        try:
            with pytest.raises(HTTPException) as error:
                start((other, user))
            assert error.value.detail['code'] == 'AI_REQUEST_IN_PROGRESS'
        finally:
            release.set()
        first = pending.result(timeout=5)
    assert start((other, user)) == first
    assert provider.calls == 1


def test_quota_is_persistent_and_cached_retry_does_not_spend_it(service):
    s, user = service
    first = start(service)
    with s.db.transaction() as db:
        db.execute("UPDATE ai_provider_operations SET created_at=strftime('%s','now')")
        for index in range(19):
            db.execute("INSERT INTO ai_provider_operations VALUES(?,?,'GENERATE',?,'hash','FINISHED',strftime('%s','now'))", (f'limit-{index}', user, f'limit-{index}'))
    assert start(service) == first
    with pytest.raises(HTTPException) as error:
        start(service, 'new-operation')
    assert error.value.detail['code'] == 'AI_QUOTA_EXCEEDED'


def test_provider_configuration_is_explicit(tmp_path, monkeypatch):
    monkeypatch.setenv('AI_PROVIDER_TYPE', 'unconfigured')
    assert not create_app(Settings(tmp_path)).state.ai_practice.provider.is_configured()
    monkeypatch.setenv('AI_PROVIDER_TYPE', 'fake')
    app = create_app(Settings(tmp_path))
    with TestClient(app) as client:
        assert client.get('/ai-practice/config').json()['provider'] == 'demo'
