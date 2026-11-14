package com.example.mentor.schedule.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Calendar

@Composable
fun DaySelectorStrip(
    days: List<String>,
    selectedDay: Int,
    onDaySelected: (Int) -> Unit
) {
    val currentDayOfWeek = getCurrentDayOfWeek()

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        days.take(7).forEachIndexed { index, dayName ->
            val dayNumber = index + 1
            val isSelected = selectedDay == dayNumber
            val isToday = currentDayOfWeek == dayNumber

            val bgColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
            val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

            Surface(
                onClick = { onDaySelected(dayNumber) },
                shape = RoundedCornerShape(14.dp),
                color = bgColor,
                modifier = Modifier.weight(1f).heightIn(min = 54.dp)
                    .semantics { selected = isSelected }
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = dayName,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = contentColor
                    )
                    
                    // Indicator for today
                    if (isToday) {
                        Box(Modifier.size(4.dp).background(contentColor, CircleShape))
                    } else {
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}

private fun getCurrentDayOfWeek(): Int {
    val cal = Calendar.getInstance()
    var day = cal.get(Calendar.DAY_OF_WEEK) - 1
    if (day == 0) day = 7
    return day
}
