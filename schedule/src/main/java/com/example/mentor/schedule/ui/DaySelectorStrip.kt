package com.example.mentor.schedule.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(days) { index, dayName ->
            val dayNumber = index + 1
            val isSelected = selectedDay == dayNumber
            val isToday = currentDayOfWeek == dayNumber

            val bgColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
            val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = bgColor,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onDaySelected(dayNumber) }
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp).widthIn(min = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = dayName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = contentColor
                    )
                    
                    // Indicator for today
                    if (isToday) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .background(contentColor, RoundedCornerShape(2.dp))
                        )
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