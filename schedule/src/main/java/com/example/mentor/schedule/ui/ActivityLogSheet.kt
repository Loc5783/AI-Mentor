package com.example.mentor.schedule.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mentor.schedule.domain.ScheduleActivityLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityLogSheet(
    logs: List<ScheduleActivityLog>,
    onDismiss: () -> Unit,
    onClearLogs: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Lịch sử hoạt động", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (logs.isNotEmpty()) {
                    TextButton(onClick = onClearLogs) {
                        Text("Dọn dẹp", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            
            if (logs.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    Text("Chưa có ghi nhận nào.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    items(logs, key = { it.id }) { log ->
                        LogItem(log)
                    }
                }
            }
        }
    }
}

@Composable
private fun LogItem(log: ScheduleActivityLog) {
    val formatter = SimpleDateFormat("HH:mm - dd/MM", Locale.getDefault())
    val timeStr = formatter.format(Date(log.timestamp))
    
    val dotColor = when (log.actionType) {
        "CREATE" -> MaterialTheme.colorScheme.primary
        "UPDATE" -> MaterialTheme.colorScheme.tertiary
        "DELETE" -> MaterialTheme.colorScheme.error
        "COMPLETE" -> androidx.compose.ui.graphics.Color(0xFF10B981)
        else -> MaterialTheme.colorScheme.outline
    }

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier.padding(top = 4.dp).size(10.dp).background(dotColor, CircleShape)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = log.description,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = timeStr,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}