package com.example.asm_final.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.asm_final.R
import com.example.asm_final.learning.OfflineLessonCatalog

/** Read-only fallback when online lessons cannot load. */
@Composable
internal fun OfflineLessonContent(subjectId: String, grade: Int) {
    val context = LocalContext.current
    val course = remember(subjectId, grade) {
        runCatching {
            context.assets.open("offline/$subjectId-$grade.json").bufferedReader(Charsets.UTF_8).use {
                OfflineLessonCatalog.parse(it.readText(), subjectId, grade)
            }
        }.getOrNull()
    }
    if (course == null) {
        Text(stringResource(R.string.course_offline_unavailable))
        return
    }
    var selected by remember(subjectId, grade) { mutableStateOf<String?>(null) }
    val lesson = course.lessons.firstOrNull { it.id == selected }
    if (lesson == null) {
        Text(stringResource(R.string.course_offline_title), style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold)
        course.lessons.forEachIndexed { index, item ->
            Surface(Modifier.fillMaxWidth().clickable { selected = item.id },
                shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp) {
                Text("${index + 1}. ${item.title}", Modifier.padding(14.dp),
                    style = MaterialTheme.typography.bodyLarge)
            }
        }
    } else {
        TextButton(onClick = { selected = null }) { Text(stringResource(R.string.course_offline_back)) }
        Text(lesson.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        lesson.guidance.forEach { paragraph ->
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
                Text(paragraph, Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge)
            }
        }
        if (lesson.examples.isNotEmpty()) {
            Text(stringResource(R.string.course_offline_examples),
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            lesson.examples.forEach { example -> Text(example, style = MaterialTheme.typography.bodyMedium) }
        }
    }
}
