package com.example.asm_final.learning

import android.content.Context
import java.security.MessageDigest

interface SessionPointerStore {
    fun getActiveSessionId(endpoint: String, owner: Int, subjectId: String, grade: Int, quizId: String): String?
    fun setActiveSessionId(endpoint: String, owner: Int, subjectId: String, grade: Int, quizId: String, sessionId: String)
    fun clearActiveSessionId(endpoint: String, owner: Int, subjectId: String, grade: Int, quizId: String)
}

class SharedPreferencesSessionPointerStore(context: Context) : SessionPointerStore {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun getActiveSessionId(
        endpoint: String,
        owner: Int,
        subjectId: String,
        grade: Int,
        quizId: String
    ): String? {
        val key = buildKey(endpoint, owner, subjectId, grade, quizId)
        return prefs.getString(key, null)
    }

    override fun setActiveSessionId(
        endpoint: String,
        owner: Int,
        subjectId: String,
        grade: Int,
        quizId: String,
        sessionId: String
    ) {
        val key = buildKey(endpoint, owner, subjectId, grade, quizId)
        prefs.edit().putString(key, sessionId).apply()
    }

    override fun clearActiveSessionId(
        endpoint: String,
        owner: Int,
        subjectId: String,
        grade: Int,
        quizId: String
    ) {
        val key = buildKey(endpoint, owner, subjectId, grade, quizId)
        prefs.edit().remove(key).apply()
    }

    companion object {
        private const val PREFS_NAME = "aim_session_pointers"

        fun buildKey(endpoint: String, owner: Int, subjectId: String, grade: Int, quizId: String): String {
            val epHash = hashString(endpoint.trim().lowercase())
            val cleanSub = subjectId.trim().lowercase()
            val cleanQuiz = quizId.trim()
            return "ptr_${epHash}_u${owner}_${cleanSub}_g${grade}_q${cleanQuiz}"
        }

        private fun hashString(input: String): String {
            val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
            return bytes.joinToString("") { "%02x".format(it) }.take(16)
        }
    }
}

class InMemorySessionPointerStore : SessionPointerStore {
    private val store = mutableMapOf<String, String>()

    override fun getActiveSessionId(
        endpoint: String,
        owner: Int,
        subjectId: String,
        grade: Int,
        quizId: String
    ): String? {
        val key = SharedPreferencesSessionPointerStore.buildKey(endpoint, owner, subjectId, grade, quizId)
        return store[key]
    }

    override fun setActiveSessionId(
        endpoint: String,
        owner: Int,
        subjectId: String,
        grade: Int,
        quizId: String,
        sessionId: String
    ) {
        val key = SharedPreferencesSessionPointerStore.buildKey(endpoint, owner, subjectId, grade, quizId)
        store[key] = sessionId
    }

    override fun clearActiveSessionId(
        endpoint: String,
        owner: Int,
        subjectId: String,
        grade: Int,
        quizId: String
    ) {
        val key = SharedPreferencesSessionPointerStore.buildKey(endpoint, owner, subjectId, grade, quizId)
        store.remove(key)
    }

    fun clearAll() {
        store.clear()
    }
}
