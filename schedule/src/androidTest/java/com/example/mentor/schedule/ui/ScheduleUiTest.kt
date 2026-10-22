package com.example.mentor.schedule.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.mentor.schedule.data.ScheduleRepositoryImpl
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScheduleUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var repository: ScheduleRepositoryImpl

    @Before
    fun setup() {
        context.deleteDatabase("AIMentorSchool.db")
        repository = ScheduleRepositoryImpl(context)
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
        
        // Verify delete and undo appears
        composeTestRule.onNodeWithText("Updated Subject").assertDoesNotExist()
        composeTestRule.onNodeWithText("Undo").assertIsDisplayed()
        
        // Undo
        composeTestRule.onNodeWithText("Undo").performClick()
        composeTestRule.onNodeWithText("Updated Subject").assertIsDisplayed()
    }
}