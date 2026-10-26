package com.example.asm_final.learning

import android.content.Context
import android.content.Intent

data class LessonHandoffContract(
    val username: String?,
    val chatThreadId: String,
    val lessonReturn: Boolean = true
) {
    companion object {
        const val EXTRA_USERNAME = "USERNAME"
        const val EXTRA_CHAT_THREAD_ID = "CHAT_THREAD_ID"
        const val EXTRA_LESSON_RETURN = "LESSON_RETURN"
    }

    /**
     * Serializes contract fields to an untyped dictionary map.
     * This is the single source of truth consumed by applyToIntent() and tested in unit tests.
     */
    fun toExtraMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            EXTRA_CHAT_THREAD_ID to chatThreadId,
            EXTRA_LESSON_RETURN to lessonReturn
        )
        if (username != null) {
            map[EXTRA_USERNAME] = username
        }
        return map
    }

    /**
     * Applies the contract dictionary directly to an Android Intent.
     */
    fun applyToIntent(intent: Intent): Intent {
        for ((key, value) in toExtraMap()) {
            when (value) {
                is String -> intent.putExtra(key, value)
                is Boolean -> intent.putExtra(key, value)
            }
        }
        return intent
    }
}

object LessonChatHandoff {
    const val EXTRA_USERNAME = LessonHandoffContract.EXTRA_USERNAME
    const val EXTRA_CHAT_THREAD_ID = LessonHandoffContract.EXTRA_CHAT_THREAD_ID
    const val EXTRA_LESSON_RETURN = LessonHandoffContract.EXTRA_LESSON_RETURN

    @JvmStatic
    fun createContract(username: String?, threadId: String): LessonHandoffContract {
        return LessonHandoffContract(username = username, chatThreadId = threadId, lessonReturn = true)
    }

    @JvmStatic
    fun buildHandoffMap(username: String?, threadId: String): Map<String, Any> {
        return createContract(username, threadId).toExtraMap()
    }

    @JvmStatic
    fun createHandoffIntent(
        context: Context,
        username: String?,
        threadId: String
    ): Intent {
        val intent = Intent(context, com.example.asm_final.activities.HomeActivity::class.java)
        val contract = createContract(username, threadId)
        return contract.applyToIntent(intent)
    }

    @JvmStatic
    fun isLessonReturn(intent: Intent?): Boolean {
        return intent?.getBooleanExtra(LessonHandoffContract.EXTRA_LESSON_RETURN, false) ?: false
    }

    @JvmStatic
    fun extractThreadId(intent: Intent?): String? {
        return intent?.getStringExtra(LessonHandoffContract.EXTRA_CHAT_THREAD_ID)
    }
}
