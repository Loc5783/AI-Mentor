package com.example.asm_final.learning

import android.content.Context
import com.example.mentor.learning.SubjectId

/** A start-from-beginning decision belongs to one account, grade and subject. */
class PathChoiceStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("lesson_path_choices", Context.MODE_PRIVATE)

    fun startedFromBeginning(owner: Int, grade: Int, subject: SubjectId): Boolean =
        owner > 0 && preferences.getBoolean(key(owner, grade, subject), false)

    fun markStartedFromBeginning(owner: Int, grade: Int, subject: SubjectId): Boolean {
        require(owner > 0 && grade in 10..12)
        return preferences.edit().putBoolean(key(owner, grade, subject), true).commit()
    }

    private fun key(owner: Int, grade: Int, subject: SubjectId) = "$owner:$grade:${subject.wireId}"
}
