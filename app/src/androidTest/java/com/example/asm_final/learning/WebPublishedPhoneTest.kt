package com.example.asm_final.learning

import android.content.Intent
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.asm_final.AiMentorApplication
import com.example.asm_final.activities.LessonActivity
import com.example.asm_final.accounts.RemoteAccountLinks
import com.example.asm_final.backend.BackendClient
import com.example.asm_final.ui.LibraryPracticeScreen
import com.example.mentor.data.ActiveUser
import com.example.mentor.learning.SubjectId
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/** Requires the isolated backend and a quiz published through the real admin browser.
 * Library listing uses the product Compose screen; subsequent flows use product
 * repositories on the physical device, not simulated UI clicks.
 */
@RunWith(AndroidJUnit4::class)
class WebPublishedPhoneTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test(timeout = 120000) fun publishedBrowserQuizReachesPhoneAndProductRepositories() = runBlocking {
        assumeTrue("Requires isolated browser fixture and a coordinated outage host",
            InstrumentationRegistry.getArguments().getString("acceptanceFixture") == "PHONE_OCT29")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val services = (context.applicationContext as AiMentorApplication).services()
        val previousOwner = ActiveUser.get(context)
        val prefs = context.getSharedPreferences("backend_test", 0)
        val previousEndpoint = prefs.getString("endpoint", null)
        var scenario: ActivityScenario<LessonActivity>? = null
        try {
            prefs.edit().putString("endpoint", "http://127.0.0.1:18080").commit()
            val name = "phoneqa_${System.currentTimeMillis()}"
            val password = "Temporary-QA-284!"
            val email = "$name@example.test"
            val owner = services.registration().register(name, password, email, "0912345678").toInt()
            assertTrue(owner > 0)
            services.learners().setGrade(owner, 10)
            val identity = BackendClient.register(context, name, email, password)
            RemoteAccountLinks(context).use { it.bind(owner, identity.endpoint, identity.remoteId()) }
            BackendClient.install(context, owner, identity)
            ActiveUser.set(context, owner)
            val client = BackendClient(context, owner)
            val before = client.request("GET", "/me/dashboard?grade=10", null, null, null)
            val library = BackendLibraryRepository(context, owner)
            val quiz = library.loadQuizzes("physics", 10).single { it.title == "PHONE_OCT29 Web to Phone" }
            assertEquals(2, quiz.questionCount)

            val intent = Intent(context, LessonActivity::class.java)
                .putExtra("SUBJECT", "physics").putExtra("GRADE", 10)
            scenario = ActivityScenario.launch(intent)
            scenario.onActivity { activity ->
                activity.setContentView(ComposeView(activity).apply {
                    setContent { MaterialTheme { LibraryPracticeScreen(SubjectId.PHYSICS, 10, {}, {}) } }
                })
            }
            compose.waitUntil(20000) {
                compose.onAllNodesWithText(quiz.title).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText(quiz.title).performScrollTo().assertExists()
            // End the UI listing before repository-driven exercise to avoid mixed ownership.
            scenario.close()
            scenario = null
            var session = library.startSession(quiz.id)
            assertTrue(session.questions.all { it.correctOption == null })
            library.submitAnswer(session.id, session.questions[0].id, "A")
            library.submitAnswer(session.id, session.questions[1].id, "B")
            session = library.completeSession(session.id)
            assertEquals(1, session.correctCount)
            assertEquals("COMPLETED", session.state)
            assertEquals(session, library.getSession(session.id))
            val wrong = library.loadWrongQuestions("physics", 10).single { it.questionId == session.questions[1].id }
            assertEquals(4, wrong.options.size)
            assertTrue(library.retryWrongQuestion(wrong.id, "A").isCorrect)
            val after = client.request("GET", "/me/dashboard?grade=10", null, null, null)
            for (field in listOf("dailyXp", "totalXp", "streakDays")) {
                assertEquals(field, before.getInt(field), after.getInt(field))
            }
            // Host stops/restarts only the isolated backend upon these markers.
            // Keep endpoint/account identity unchanged during the outage.
            fun signal(message: String) {
                InstrumentationRegistry.getInstrumentation().sendStatus(2,
                    android.os.Bundle().apply { putString("stream", message + "\n") })
            }
            signal("PHONE_NETWORK_DROP_READY")
            Thread.sleep(3000)
            var unavailable = false
            try { library.getSession(session.id) } catch (_: java.io.IOException) { unavailable = true }
            signal("PHONE_NETWORK_RESTORE_READY")
            Thread.sleep(3000)
            assertTrue("Unavailable endpoint must surface a transport failure", unavailable)
            assertEquals(session.id, library.getSession(session.id).id)

            val exams = BackendExamRepository(context, owner)
            val exam = exams.listExams("physics", 10, "MIDTERM", 1).single { it.title == "PHONE_OCT29 Exam" }
            var examSession = exams.startSession(exam.id)
            assertTrue(examSession.questions.all { it.correctOption == null })
            exams.saveAnswer(examSession.id, examSession.questions[0].id, "A", 1)
            examSession = exams.submitSession(examSession.id)
            assertEquals("SUBMITTED", examSession.state)
            assertEquals(examSession.submittedAt, exams.submitSession(examSession.id).submittedAt)
            val afterExam = client.request("GET", "/me/dashboard?grade=10", null, null, null)
            for (field in listOf("dailyXp", "totalXp", "streakDays")) {
                assertEquals(field, before.getInt(field), afterExam.getInt(field))
            }
            // Complete the published introductory course's placement and first
            // lesson over the phone transport; fixture answers are from that pack.
            val answerKey = JSONObject("""{"physics-10-position-q1": "a", "physics-10-position-q2": "c", "physics-10-position-q3": "b", "physics-10-displacement-q1": "c", "physics-10-displacement-q2": "b", "physics-10-displacement-q3": "a", "physics-10-speed-q1": "b", "physics-10-speed-q2": "a", "physics-10-speed-q3": "c", "physics-10-velocity-q1": "a", "physics-10-velocity-q2": "c", "physics-10-velocity-q3": "b", "physics-10-acceleration-q1": "c", "physics-10-acceleration-q2": "b", "physics-10-acceleration-q3": "a"}""")
            fun finish(attempt: JSONObject): JSONObject {
                val answers = org.json.JSONArray()
                val questions = attempt.getJSONArray("questions")
                for (i in 0 until questions.length()) {
                    val qid = questions.getJSONObject(i).getString("id")
                    answers.put(JSONObject().put("questionId", qid)
                        .put("selectedOptionId", answerKey.getString(qid))
                        .put("submissionId", UUID.randomUUID().toString()))
                }
                client.request("PATCH", "/attempts/${attempt.getString("id")}",
                    JSONObject().put("step", 2).put("answers", answers), null, attempt.getInt("revision"))
                return client.request("POST", "/completions", JSONObject().put("attemptId", attempt.getString("id")), UUID.randomUUID().toString(), null)
            }
            val enrollment = client.request("POST", "/enrollments", JSONObject().put("subjectId", "physics").put("grade", 10), UUID.randomUUID().toString(), null)
            val placement = client.request("POST", "/placements", JSONObject().put("enrollmentId", enrollment.getString("id")), UUID.randomUUID().toString(), null)
            assertEquals(0, finish(placement).getInt("xpAwarded"))
            val lesson = client.request("POST", "/attempts", JSONObject().put("enrollmentId", enrollment.getString("id"))
                .put("lessonId", "physics-10-position").put("curriculumVersion", "intro-2026.1"), UUID.randomUUID().toString(), null)
            val completion = finish(lesson)
            assertEquals(200, completion.getInt("xpAwarded"))
            val restored = client.request("GET", "/attempts/${lesson.getString("id")}", null, null, null)
            assertTrue(restored.has("completion"))
            val rewards = client.request("GET", "/me/dashboard?grade=10", null, null, null)
            assertEquals(200, rewards.getInt("totalXp"))
            assertEquals(1, rewards.getInt("streakDays"))
            val ai = BackendAiPracticeRepository(context, owner)
            assertEquals("demo", ai.getConfig().provider)
            val generated = ai.createSession("physics", 10, "Phone transport verification", "EASY", 2, UUID.randomUUID().toString())
            ai.submitAnswerOperation(generated.id, generated.questions[0].id, "A", UUID.randomUUID().toString())
            ai.skipQuestion(generated.id, generated.questions[1].id)
            assertEquals("COMPLETED", ai.completeSession(generated.id).state)
        } finally {
            scenario?.close()
            ActiveUser.set(context, previousOwner)
            if (previousEndpoint == null) prefs.edit().remove("endpoint").commit()
            else prefs.edit().putString("endpoint", previousEndpoint).commit()
        }
    }
}
