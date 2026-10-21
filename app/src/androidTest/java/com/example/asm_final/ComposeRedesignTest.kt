package com.example.asm_final

import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.asm_final.activities.HomeActivity
import com.example.asm_final.chat.ChatViewModel
import com.example.asm_final.learning.LearningSetupStore
import com.example.asm_final.onboarding.LearnerProfile
import com.example.asm_final.utils.LocaleHelper
import com.example.asm_final.utils.ThemeHelper
import com.example.mentor.data.ActiveUser
import com.example.mentor.learning.SubjectId
import org.junit.*
import org.junit.runner.RunWith
import org.junit.rules.RuleChain

/** Real hosts and repositories, no paid AI calls or shared backend test writes. */
@RunWith(AndroidJUnit4::class)
class ComposeRedesignTest {
    private val compose = createEmptyComposeRule()
    @get:Rule val rules: RuleChain = RuleChain.outerRule(LocalBackendIsolation()).around(compose)
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private lateinit var app: AiMentorApplication
    private lateinit var scenario: ActivityScenario<HomeActivity>
    private var owner = 0
    private var previous = 0
    private var originalDark = false
    private var originalLanguage = "vi"

    @Before fun prepare() {
        app = context.applicationContext as AiMentorApplication
        previous = ActiveUser.get(context)
        originalDark = ThemeHelper.isDarkMode(context)
        originalLanguage = LocaleHelper.getLanguage(context)
        val username = "compose_qa_${System.nanoTime()}"
        owner = app.services().registration().register(username, "Mentor01!2026", "qa@gmail.com", "0901234567").toInt()
        Assert.assertTrue(owner > 0)
        app.services().learners().complete(LearnerProfile(owner, 5, "Compose QA", 11, "BIOLOGY,PHYSICS", 0, 0, 0))
        LearningSetupStore(context).use { it.save(owner, 11, setOf(SubjectId.BIOLOGY, SubjectId.PHYSICS)) }
        ActiveUser.set(context, owner)
        scenario = ActivityScenario.launch(Intent(context, HomeActivity::class.java).putExtra("USERNAME", username))
        waitForTag("dashboard-subject:biology")
    }

    @After fun cleanup() {
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

    private fun waitForNativeSetting(id: Int, language: String? = null) {
        val end = android.os.SystemClock.uptimeMillis() + 15000
        while (android.os.SystemClock.uptimeMillis() < end) {
            var found = false
            instrumentation.runOnMainSync {
                androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance()
                    .getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED).forEach { activity ->
                        if (activity is HomeActivity && activity.findViewById<android.view.View>(id) != null && activity.hasWindowFocus()
                            && (language == null || activity.resources.configuration.locales[0].language == language)) found = true
                    }
            }
            if (found) return
            android.os.SystemClock.sleep(40)
        }
        Assert.fail("Settings did not resume")
    }
    private fun waitForTag(tag: String) {
        compose.waitUntil(15000) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
    }
    private fun capture(name: String) {
        compose.waitForIdle()
        val bitmap = instrumentation.uiAutomation.takeScreenshot() ?: return
        java.io.FileOutputStream(java.io.File(context.getExternalFilesDir(null), "compose-$name.png")).use {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
        bitmap.recycle()
        val file = java.io.File(context.getExternalFilesDir(null), "compose-$name.png")
        android.os.ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(
            "cp ${file.absolutePath} /data/local/tmp/compose-$name.png")).use { it.readBytes() }
    }
    private fun navigate(id: Int) { scenario.onActivity { it.navigateTo(id, false) }; compose.waitForIdle() }
    private fun label(id: Int): String {
        var value = ""
        instrumentation.runOnMainSync { value = context.getString(id) }
        return value
    }

    @Test(timeout = 60000) fun fiveTabsAndEmptyChartHaveUsefulContent() {
        listOf(R.string.design_tab_home, R.string.design_tab_chat, R.string.design_tab_lessons, R.string.design_tab_competition, R.string.nav_profile_label).forEach {
            compose.onAllNodesWithText(label(it)).onFirst().assertExists()
        }
        capture("home")
        compose.onNodeWithTag("dashboard-subject:physics").assertExists()
        compose.onNodeWithTag("dashboard-subject:chemistry").assertDoesNotExist()
        compose.onAllNodesWithText(label(R.string.design_chart_hint)).onFirst().assertExists()
        navigate(R.id.nav_competition)
        compose.onNodeWithText(label(R.string.competition_title)).assertExists()
        navigate(R.id.nav_profile)
        compose.onNodeWithText(label(R.string.settings_title)).assertExists()
        compose.onNodeWithText(label(R.string.profile_edit)).assertExists()
        capture("profile")
    }

    @Test(timeout = 60000) fun chatDraftSurvivesTabsAndRecreation() {
        navigate(R.id.nav_chat)
        waitForTag("chat-input")
        compose.waitUntil(15000) { runCatching { compose.onNodeWithTag("chat-input").assertIsEnabled(); true }.getOrDefault(false) }
        compose.onNodeWithTag("chat-input").performTextInput("Giữ câu hỏi đang soạn")
        navigate(R.id.nav_home)
        navigate(R.id.nav_chat)
        compose.onNodeWithTag("chat-input").assertTextContains("Giữ câu hỏi đang soạn")
        scenario.recreate()
        waitForTag("chat-input")
        compose.onNodeWithTag("chat-input").assertTextContains("Giữ câu hỏi đang soạn")
    }

    @Test(timeout = 60000) fun subjectChangesAndToolsSurviveRecreation() {
        LearningSetupStore(context).use { it.save(owner, 11, setOf(SubjectId.CHEMISTRY, SubjectId.PHYSICS)) }
        navigate(R.id.nav_category)
        waitForTag("subject-list")
        compose.onNodeWithTag("subject-list").performScrollToNode(hasTestTag("subject:physics"))
        compose.onNodeWithText(label(R.string.lesson_circuit)).assertExists()
        scenario.recreate()
        waitForTag("subject-list")
        compose.onNodeWithTag("subject-list").performScrollToNode(hasTestTag("subject:physics"))
        compose.onNodeWithText(label(R.string.lesson_circuit)).assertExists()
        compose.onNodeWithTag("subject-list").performScrollToNode(hasTestTag("subject:chemistry"))
        compose.onNodeWithTag("subject:chemistry").assertExists()
        navigate(R.id.nav_home)
        waitForTag("dashboard-subject:chemistry")
        compose.onNodeWithTag("dashboard-subject:biology").assertDoesNotExist()
    }

    @Test(timeout = 60000) fun languageAndDarkModeKeepChatDraft() {
        navigate(R.id.nav_chat)
        waitForTag("chat-input")
        compose.waitUntil(15000) { runCatching { compose.onNodeWithTag("chat-input").assertIsEnabled(); true }.getOrDefault(false) }
        compose.onNodeWithTag("chat-input").performTextInput("Draft through theme and language")
        // Exercise the actual settings workflow: users leave chat before changing these options.
        navigate(R.id.nav_settings)
        scenario.onActivity { it.findViewById<android.view.View>(R.id.switch_dark_mode).performClick() }
        waitForNativeSetting(R.id.setting_language)
        scenario.onActivity { it.findViewById<android.view.View>(R.id.setting_language).performClick() }
        val languageLabel = context.getString(if (originalLanguage == "en") R.string.language_vietnamese else R.string.language_english)
        androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withText(languageLabel))
            .perform(androidx.test.espresso.action.ViewActions.click())
        waitForNativeSetting(R.id.setting_language, if (originalLanguage == "en") "vi" else "en")
        Assert.assertEquals(if (originalLanguage == "en") "vi" else "en", LocaleHelper.getLanguage(context))
        Assert.assertEquals(!originalDark, ThemeHelper.isDarkMode(context))
        navigate(R.id.nav_chat)
        waitForTag("chat-input")
        compose.onNodeWithTag("chat-input").assertTextContains("Draft through theme and language")
        compose.onNodeWithContentDescription(label(R.string.chat_history)).assertExists()
        capture("chat-dark-language")
    }
}
