package com.example.asm_final.learning

import android.content.Context
import com.example.mentor.learning.SubjectId
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Synchronous persistence boundary. Implementations must not decide which UI state to show. */
internal interface SubjectEnrollmentDataSource {
    fun selected(ownerId: Int, grade: Int): Set<SubjectId>
    fun save(ownerId: Int, grade: Int, subjects: Set<SubjectId>)
}

internal class LearningSetupEnrollmentDataSource(context: Context) : SubjectEnrollmentDataSource {
    private val appContext = context.applicationContext

    override fun selected(ownerId: Int, grade: Int): Set<SubjectId> =
        LearningSetupStore(appContext).use { it.selected(ownerId, grade) }

    override fun save(ownerId: Int, grade: Int, subjects: Set<SubjectId>) {
        LearningSetupStore(appContext).use { it.save(ownerId, grade, subjects) }
    }
}

/** Runs database and Supabase work off the UI thread and propagates failures to the caller. */
internal class SubjectEnrollmentRepository(
    private val dataSource: SubjectEnrollmentDataSource,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    suspend fun selected(ownerId: Int, grade: Int): Set<SubjectId> =
        withContext(ioDispatcher) { dataSource.selected(ownerId, grade) }

    suspend fun save(ownerId: Int, grade: Int, subjects: Set<SubjectId>) {
        withContext(ioDispatcher) { dataSource.save(ownerId, grade, subjects) }
    }
}