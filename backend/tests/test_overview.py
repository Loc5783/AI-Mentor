from test_learning import api, enroll, placed, start, answer, finish
from test_api import headers, signup, key


def overview(c,s,grade=10):
    response=c.get("/me/dashboard",params={"grade":grade},headers=headers(s))
    assert response.status_code==200,response.text
    return response.json()


def test_fresh_account_has_no_fabricated_progress(api):
    c,_,s,_=api
    result=overview(c,s)
    assert result["totalXp"]==result["dailyXp"]==result["completedLessons"]==result["streakDays"]==0
    assert result["practice"]["questionCount"]==0
    assert len(result["week"])==7 and all(day["xp"]==0 for day in result["week"])


def test_completion_changes_all_stats_and_replay_does_not_inflate_them(api):
    c,_,s,_=api
    e=enroll(c,s)
    placed(c,s,e)
    before=overview(c,s)
    assert before["totalLessons"]==10 and before["totalXp"]==0
    a=answer(c,s,start(c,s,e,"physics-10-unit-1"))
    finish(c,s,a)
    result=overview(c,s)
    assert (result["totalXp"],result["dailyXp"],result["completedLessons"],result["streakDays"])==(200,200,1,1)
    assert result["practice"]["firstTryCorrect"]==result["practice"]["questionCount"]==3
    finish(c,s,a)
    again=overview(c,s)
    assert again["totalXp"]==200 and again["completedLessons"]==1


def test_retake_updates_daily_streak_without_double_counting_lessons_or_accuracy(api):
    c,_,s,now=api
    e=enroll(c,s);placed(c,s,e)
    finish(c,s,answer(c,s,start(c,s,e,"physics-10-unit-1")))
    now[0]+=120
    finish(c,s,answer(c,s,start(c,s,e,"physics-10-unit-1")))
    result=overview(c,s)
    assert (result["totalXp"],result["dailyXp"],result["streakDays"],result["completedLessons"])==(200,0,2,1)
    assert result["practice"]["questionCount"]==3
    assert result["week"][-1]["streakLit"] and result["week"][-1]["xp"]==0


def test_owner_and_grade_do_not_share_completed_counts(api):
    c,_,s,_=api
    other=signup(c,"AnotherLearner")
    e=enroll(c,s);placed(c,s,e)
    finish(c,s,answer(c,s,start(c,s,e,"physics-10-unit-1")))
    assert overview(c,other)["totalXp"]==0
    different=overview(c,s,11)
    assert different["completedLessons"]==0 and different["practice"]["questionCount"]==0
    assert different["totalXp"]==200  # lifetime EXP does not disappear when changing grade
    assert c.get("/me/dashboard",params={"grade":9},headers=headers(s)).status_code==422


def test_correct_after_guidance_is_not_reported_as_first_try_mastery(api):
    c,_,s,_=api
    e=enroll(c,s);placed(c,s,e)
    a=start(c,s,e,"physics-10-unit-1")
    qid=a["questions"][0]["id"]
    for _ in range(2):
        a=answer(c,s,a,[{"questionId":qid,"selectedOptionId":"b","submissionId":key()}])
    finish(c,s,answer(c,s,a))
    result=overview(c,s)
    assert result["totalXp"]==200 and result["completedLessons"]==1
    assert result["practice"]["firstTryCorrect"]==2
    assert result["practice"]["questionCount"]==3
    assert result["practice"]["assisted"]==1


def test_public_profile_and_rankings_use_the_same_learning_day_as_dashboard(api):
    c,_,s,now=api
    e=enroll(c,s);placed(c,s,e)
    finish(c,s,answer(c,s,start(c,s,e,"physics-10-unit-1")))
    for expected in (200,0):
        result=overview(c,s)
        public=c.get("/me/public-profile",headers=headers(s)).json()
        ranking=c.get("/rankings",params={"metric":"DAILY_XP","scope":"GLOBAL"},headers=headers(s)).json()
        own=next(row for row in ranking["entries"] if row["profile"]["id"]==s["userId"])
        assert public["dailyXp"]==own["value"]==result["dailyXp"]==expected
        assert public["streakDays"]==result["streakDays"]
        assert public["learningDay"]==ranking["learningDay"]==result["learningDay"]
        now[0]+=120
