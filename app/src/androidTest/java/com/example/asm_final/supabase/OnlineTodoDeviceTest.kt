package com.example.asm_final.supabase

import androidx.test.platform.app.InstrumentationRegistry
import com.example.mentor.data.ActiveUser
import com.example.mentor.schedule.domain.TodoDates
import com.example.mentor.schedule.domain.TodoTask
import org.json.JSONObject
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class OnlineTodoDeviceTest {
    @Test fun datedTodoIsStoredOnlyOnItsDueDate() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val owner = ActiveUser.get(context)
        assertTrue("Sign in with the test account first", owner > 0)
        val account = owner.toString()
        val day = ((Calendar.getInstance().get(Calendar.DAY_OF_WEEK) + 5) % 7) + 1
        val due = TodoDates.today()
        val title = "QA-TODO-${System.currentTimeMillis()}"
        val repository = SupabaseScheduleRepository(context)
        var id = 0L
        try {
            id = repository.insertTodo(TodoTask(accountId = account, title = title,
                dayOfWeek = day, dueDate = due, planStartDate = due))
            assertTrue(repository.getTodos(account, day).any { it.id == id && it.title == title })
            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
            val next = Calendar.getInstance().apply { time = formatter.parse(due)!!; add(Calendar.DAY_OF_YEAR, 7) }
            val data = JSONObject().put("day", day).put("date", formatter.format(next.time))
            val body = JSONObject().put("op", "todos").put("data", data)
            val response = JSONObject(SupabaseAccounts(context).requestFor(owner, "POST",
                "/rest/v1/rpc/mentor_schedule_action", body))
            val items = response.getJSONArray("items")
            assertFalse((0 until items.length()).any { items.getJSONObject(it).getLong("id") == id })
        } finally {
            if (id > 0) repository.deleteTodo(id, account)
        }
    }
}
