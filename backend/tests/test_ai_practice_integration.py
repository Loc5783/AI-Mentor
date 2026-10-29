"""Cross-feature contracts: AI sessions, notebook and completion."""
from test_ai_practice_safety import service, start
from aimentor.library import LibraryService


def test_ai_wrong_answer_is_readable_and_retryable_in_shared_notebook(service):
    ai, user = service
    session = start(service)
    qid = session['questions'][0]['id']
    ai.submit_answer(user, session['id'], qid, 'B', 'wrong')
    library = LibraryService(ai.db)
    entries = library.get_wrong_questions(user)
    assert len(entries) == 1
    assert isinstance(entries[0]['options'], list)
    assert [o['id'] for o in entries[0]['options']] == ['A', 'B', 'C', 'D']
    assert all(o['text'] for o in entries[0]['options'])
    result = library.retry_wrong_question(user, entries[0]['id'], 'A')
    assert result['state'] == 'RESOLVED'
    assert ai.get_session(user, session['id'])['answers'][qid]['firstTryCorrect'] is False


def test_skipped_ai_question_is_recorded_once_in_shared_notebook(service):
    ai, user = service
    session = start(service)
    qid = session['questions'][0]['id']
    ai.skip_question(user, session['id'], qid)
    ai.skip_question(user, session['id'], qid)
    entries = LibraryService(ai.db).get_wrong_questions(user)
    assert len(entries) == 1
    assert entries[0]['questionId'] == qid
    assert entries[0]['source'] == 'AI_PRACTICE'
    assert len(entries[0]['options']) == 4


def test_completion_replay_preserves_original_summary(service):
    ai, user = service
    session = start(service)
    first = ai.complete_session(user, session['id'])
    second = ai.complete_session(user, session['id'])
    assert second['summary'] == first['summary']
    assert second['updatedAt'] == first['updatedAt']
