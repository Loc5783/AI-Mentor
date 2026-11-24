package com.example.asm_final.learning

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.asm_final.*
import com.example.asm_final.R
import com.example.asm_final.activities.HomeActivity
import com.example.asm_final.onboarding.LearnerProfile
import com.example.asm_final.utils.LocaleHelper
import com.example.asm_final.utils.ThemeHelper
import com.example.mentor.data.ActiveUser
import com.example.mentor.learning.SubjectId
import org.junit.*
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class LearnAiChatNavigationDeviceTest {
    private val compose = createEmptyComposeRule()
    @get:Rule val rules: RuleChain = RuleChain.outerRule(LocalBackendIsolation()).around(compose)

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private lateinit var app: AiMentorApplication
    private lateinit var scenario: ActivityScenario<HomeActivity>
    private lateinit var qaUsername: String
    private var owner = 0
    private var previous = 0
    private var originalDark = false
    private var originalLanguage = "vi"

    @Before
    fun prepare() {
        app = context.applicationContext as AiMentorApplication
        previous = ActiveUser.get(context)
        originalDark = ThemeHelper.isDarkMode(context)
        originalLanguage = LocaleHelper.getLanguage(context)
        qaUsername = "learn_qa_${System.nanoTime()}"
        owner = app.services().registration().register(qaUsername, "Mentor01!2026", "learnqa@gmail.com", "0901234567").toInt()
        Assert.assertTrue(owner > 0)
        app.services().learners().complete(LearnerProfile(owner, 5, "Learn QA", 10, "PHYSICS,MATH", 0, 0, 0))
        LearningSetupStore(context).use { it.save(owner, 10, setOf(SubjectId.PHYSICS, SubjectId.MATH)) }
        ActiveUser.set(context, owner)
        scenario = ActivityScenario.launch(Intent(context, HomeActivity::class.java).putExtra("USERNAME", qaUsername))
        waitForTag("home-progress-hero")
    }

    @After
    fun cleanup() {
        if (::scenario.isInitialized) scenario.close()
        instrumentation.runOnMainSync {
            ThemeHelper.setDarkMode(context, originalDark)
            LocaleHelper.setLocale(context, originalLanguage)
        }
        ActiveUser.set(context, previous)
        com.example.asm_final.activities.database.DatabaseHelper(context).use {
            it.writableDatabase.delete("users", "id=?", arrayOf(owner.toString()))
        }
    }

    private fun waitForTag(tag: String) {
        compose.waitUntil(15000) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun waitForText(text: String) {
        compose.waitUntil(15000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun navigate(id: Int) {
        scenario.onActivity { it.navigateTo(id, false) }
        compose.waitForIdle()
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        val bitmap = instrumentation.uiAutomation.takeScreenshot() ?: return
        val outDir = File(context.getExternalFilesDir(null), "screenshots")
        outDir.mkdirs()
        val file = File(outDir, "$name.png")
        FileOutputStream(file).use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        bitmap.recycle()
        instrumentation.uiAutomation.executeShellCommand("mkdir -p /sdcard/screenshots")
        instrumentation.uiAutomation.executeShellCommand("cp ${file.absolutePath} /sdcard/screenshots/$name.png")
    }

    private fun label(id: Int): String {
        var value = ""
        instrumentation.runOnMainSync { value = context.getString(id) }
        return value
    }

    @Test(timeout = 60000)
    fun testCompleteLearnAndAiChatFlow() {
        // Verify the five destinations; the study plan is inside Study.
        val homeLabel = label(R.string.design_tab_home)
        val lessonsLabel = label(R.string.design_tab_lessons)
        val teamLabel = label(R.string.team_tab_title)
        val competitionLabel = label(R.string.design_tab_competition)
        val profileLabel = label(R.string.nav_profile_label)

        compose.onAllNodesWithText(homeLabel).onFirst().assertExists()
        compose.onAllNodesWithText(lessonsLabel).onFirst().assertExists()
        compose.onAllNodesWithText(teamLabel).onFirst().assertExists()
        compose.onAllNodesWithText(competitionLabel).onFirst().assertExists()
        compose.onAllNodesWithText(profileLabel).onFirst().assertExists()

        capture("bottom_navigation_bar")

        // --- 2. Navigate to Learn Tab (nav_category) ---
        navigate(R.id.nav_category)
        waitForText(label(R.string.lesson_hub_grade))

        // Grade and plan are present at the study root.
        compose.onNodeWithText(label(R.string.lesson_hub_grade)).assertExists()
        compose.onNodeWithText(label(R.string.lesson_hub_change_grade)).assertExists()
        compose.onNodeWithText(label(R.string.study_plan_title)).assertExists()
        capture("learn_top_header")

        // Tools are grouped under the selected subject.
        compose.onNodeWithText("Toán").performScrollTo().performClick()
        compose.onNodeWithText(label(R.string.study_path_title)).assertExists()
        compose.onNodeWithText(label(R.string.study_practice_group)).assertExists()
        compose.onNodeWithText(label(R.string.study_exam_group)).assertExists()
        compose.onNodeWithText(label(R.string.study_review_group)).assertExists()
        capture("learn_subject_workspace")

        compose.onNodeWithText(label(R.string.lesson_hub_chat)).performScrollTo().performClick()
        compose.waitForIdle()

        // Verify AI Chat screen is opened
        waitForTag("chat-input")
        compose.onNodeWithTag("chat-input").assertExists()
        capture("ai_chat_screen_opened")

        // --- 4. Test typing in AI Chat input ---
        compose.onNodeWithTag("chat-input").performTextInput("Xin chào AI Mentor")
        compose.waitForIdle()
        compose.onNodeWithTag("chat-input").assertTextContains("Xin chào AI Mentor")

        // --- 5. Back navigation from AI Chat returns to Learn ---
        val backLabel = label(R.string.course_back)
        compose.onNodeWithText(backLabel).performClick()
        compose.waitForIdle()

        // Back returns to the subject workspace, preserving its context.
        waitForText(label(R.string.study_path_title))
        compose.onNodeWithText(label(R.string.study_review_group)).assertExists()

        // --- 6. Tab cycle: Home -> Learn -> Ranks -> Profile -> Learn ---
        navigate(R.id.nav_home)
        waitForTag("home-progress-hero")
        navigate(R.id.nav_competition)
        waitForText(label(R.string.competition_title))
        navigate(R.id.nav_profile)
        waitForTag("profile-hero")
        navigate(R.id.nav_category)
        waitForText(label(R.string.study_path_title))
        compose.onNodeWithText(label(R.string.course_back)).performClick()
        waitForText(label(R.string.lesson_hub_grade))

        compose.onNodeWithText(label(R.string.study_manage_path)).performScrollTo().performClick()
        compose.waitForIdle()
        // Subject list in Roadmap
        waitForTag("subject-list")
        compose.onNodeWithTag("subject-list").assertExists()
    }
}
