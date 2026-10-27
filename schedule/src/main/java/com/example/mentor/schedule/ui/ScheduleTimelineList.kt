package com.example.mentor.schedule.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mentor.schedule.domain.ScheduleEntry

import java.util.Calendar

@Composable
fun ScheduleTimelineList(
    entries: List<ScheduleEntry>,
    onItemClick: (ScheduleEntry) -> Unit
) {
    if (entries.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(androidx.compose.ui.res.stringResource(com.example.mentor.schedule.R.string.schedule_empty_list), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(entries.sortedBy { it.period }, key = { it.id }) { entry ->
                ScheduleTimelineItemV2(entry, onItemClick)
            }
        }
    }
}

@Composable
private fun ScheduleTimelineItemV2(entry: ScheduleEntry, onClick: (ScheduleEntry) -> Unit) {
    val isDark = false
    val accentColor = MaterialTheme.colorScheme.primary
    val containerColor = MaterialTheme.colorScheme.primaryContainer
    val onContainerColor = MaterialTheme.colorScheme.onPrimaryContainer
    
    // Status Logic
    val status = getScheduleStatus(entry.period)
    val alphaFactor = if (status == "DONE") 0.5f else 1f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(entry) }
            .background(Color.Transparent),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Time Column
        Column(
            modifier = Modifier.width(56.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = entry.getFormattedStartTime(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = alphaFactor)
            )
            Text(
                text = entry.getFormattedEndTime(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alphaFactor)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Dot + Line Indicator
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.height(IntrinsicSize.Min)
        ) {
            val dotColor = if (status == "ONGOING") MaterialTheme.colorScheme.primary else accentColor.copy(alpha = alphaFactor)
            val dotSize = if (status == "ONGOING") 12.dp else 10.dp
            
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .background(dotColor, CircleShape)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Card Content
        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp),
            color = containerColor.copy(alpha = alphaFactor),
            border = if (status == "ONGOING") androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = entry.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = onContainerColor.copy(alpha = alphaFactor),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    
                    if (status == "ONGOING") {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                androidx.compose.ui.res.stringResource(com.example.mentor.schedule.R.string.schedule_status_ongoing),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                
                if (!entry.teacher.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${androidx.compose.ui.res.stringResource(com.example.mentor.schedule.R.string.schedule_teacher_prefix)}${entry.teacher}",
                        style = MaterialTheme.typography.bodySmall,
                        color = onContainerColor.copy(alpha = 0.8f * alphaFactor),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private fun getScheduleStatus(period: Int): String {
    val cal = Calendar.getInstance()
    val currentHour = cal.get(Calendar.HOUR_OF_DAY)
    val currentMin = cal.get(Calendar.MINUTE)
    val totalMin = currentHour * 60 + currentMin
    
    // Hardcoded logic for matching period definitions roughly
    val mapping = mapOf(
        1 to Pair(7*60, 7*60+45),
        2 to Pair(7*60+50, 8*60+35),
        3 to Pair(8*60+40, 9*60+25),
        4 to Pair(9*60+35, 10*60+20),
        5 to Pair(10*60+25, 11*60+10),
        6 to Pair(13*60, 13*60+45),
        7 to Pair(13*60+50, 14*60+35),
        8 to Pair(14*60+40, 15*60+25),
        9 to Pair(15*60+35, 16*60+20),
        10 to Pair(16*60+25, 17*60+10)
    )
    val pair = mapping[period] ?: return "UPCOMING"
    
    return when {
        totalMin > pair.second -> "DONE"
        totalMin in pair.first..pair.second -> "ONGOING"
        else -> "UPCOMING"
    }
}