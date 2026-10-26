package com.example.mentor.schedule.ui

import androidx.compose.material.icons.filled.Delete
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.mentor.schedule.domain.TodoTask

@Composable
fun DailyTodoList(
    todos: List<TodoTask>,
    progress: Float,
    onToggle: (TodoTask) -> Unit,
    onDelete: (TodoTask) -> Unit
) {
    if (todos.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Không có nhiệm vụ nào cho ngày này.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Progress
            Surface(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val completed = todos.count { it.isCompleted }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Tiến độ trong ngày", fontWeight = FontWeight.Bold)
                        Text("$completed/${todos.size}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = Color(0xFF10B981), // Emerald Green
                        trackColor = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
            
            // List Items
            LazyColumn(
                contentPadding = PaddingValues(bottom = 80.dp),
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Incomplete First
                val incomplete = todos.filter { !it.isCompleted }.sortedByDescending { it.priority }
                val complete = todos.filter { it.isCompleted }.sortedByDescending { it.completedAt }
                
                items(incomplete, key = { it.id }) { task ->
                    TodoCard(task, onToggle, onDelete)
                }
                
                if (complete.isNotEmpty()) {
                    item {
                        Text(
                            "Đã hoàn thành",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }
                    items(complete, key = { it.id }) { task ->
                        TodoCard(task, onToggle, onDelete)
                    }
                }
            }
        }
    }
}

@Composable
private fun TodoCard(task: TodoTask, onToggle: (TodoTask) -> Unit, onDelete: (TodoTask) -> Unit) {
    val alphaFactor = if (task.isCompleted) 0.6f else 1f
    
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().alpha(alphaFactor),
        shadowElevation = if (task.isCompleted) 0.dp else 1.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggle(task) },
                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF10B981))
            )
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (task.isCompleted) FontWeight.Normal else FontWeight.SemiBold,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (task.priority == 2 && !task.isCompleted) {
                    Text("Gấp", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }
            }
            
            IconButton(onClick = { onDelete(task) }) {
                Icon(
                    androidx.compose.material.icons.Icons.Default.Delete,
                    contentDescription = "Xóa nhiệm vụ",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = alphaFactor)
                )
            }
        }
    }
}