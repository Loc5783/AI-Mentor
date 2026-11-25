package com.example.mentor.schedule.ui

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.mentor.schedule.data.ScheduleRepositoryImpl
import org.junit.Before
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ScheduleUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var context: Context
    private lateinit var testDatabase: File
    private lateinit var repository: ScheduleRepositoryImpl

    @Before
    fun setup() {
        testDatabase = File(targetContext.cacheDir, "schedule-ui-test-${UUID.randomUUID()}/AIMentorSchool.db")
        context = object : ContextWrapper(targetContext) {
            override fun getApplicationContext(): Context = this
            override fun getDatabasePath(name: String): File = testDatabase
        }
        repository = ScheduleRepositoryImpl(context)
    }

    @After
    fun teardown() {
        repository.close()
        File(testDatabase.path + "-wal").delete()
        File(testDatabase.path + "-shm").delete()
        testDatabase.delete()
        testDatabase.parentFile?.delete()
    }

    @Test
    fun testAddEditDeleteSchedule() {
        composeTestRule.setContent {
            ScheduleScreen(
                accountId = "test_acc",
                repository = repository,
                onBack = {}
            )
        }

        // Add
        composeTestRule.onNodeWithContentDescription("Add Class").performClick()
        
        // Fill form
        // We look for the text field by its label
        composeTestRule.onNodeWithText("Subject Name (e.g., Math, Science)").performTextInput("Test Subject")
        composeTestRule.onNodeWithText("Save").performClick()
        
        // Verify it was added
        composeTestRule.onNodeWithText("Test Subject").assertIsDisplayed()
        
        // Edit
        composeTestRule.onNodeWithText("Test Subject").performClick()
        composeTestRule.onNodeWithText("Test Subject").performTextReplacement("Updated Subject")
        composeTestRule.onNodeWithText("Save").performClick()
        
        // Verify edit
        composeTestRule.onNodeWithText("Updated Subject").assertIsDisplayed()
        composeTestRule.onNodeWithText("Test Subject").assertDoesNotExist()
        
        // Delete
        composeTestRule.onNodeWithText("Updated Subject").performClick()
        composeTestRule.onNodeWithText("Delete").performClick()
        composeTestRule.onNodeWithText("Delete this class?").performClick()
        
        // Verify delete and undo appears
        composeTestRule.onNodeWithText("Updated Subject").assertDoesNotExist()
        composeTestRule.onNodeWithText("Undo").assertIsDisplayed()
        
        // Undo
        composeTestRule.onNodeWithText("Undo").performClick()
        composeTestRule.onNodeWithText("Updated Subject").assertIsDisplayed()
    }
}