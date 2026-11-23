package com.example.asm_final.learning

import android.net.Uri

/** Online-only account boundary for private exam packs. */
interface ExamPackStore {
    suspend fun list(): List<ExamPackFoundry.ExamPack>
    suspend fun load(id: String): ExamPackFoundry.ExamPack
    suspend fun importFile(title: String, subjectId: String, grade: Int, uri: Uri): ExamPackFoundry.ExamPack
    suspend fun review(pack: ExamPackFoundry.ExamPack, questionId: String, key: String,
                       prompt: String, options: Map<String, String>, explanation: String,
                       topic: String): ExamPackFoundry.ExamPack
}
