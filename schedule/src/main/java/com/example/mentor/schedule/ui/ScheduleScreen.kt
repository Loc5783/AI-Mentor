package com.example.mentor.schedule.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mentor.schedule.R
import com.example.mentor.schedule.domain.ScheduleEntry
import com.example.mentor.schedule.domain.ScheduleRepository
import com.example.mentor.ui.design.MentorTokens
import kotlinx.coroutines.launch

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    accountId: String,
    repository: ScheduleRepository,
    onBack: () -> Unit
) {
    val factory = remember { ScheduleViewModelFactory(accountId, repository) }
    val viewModel: ScheduleViewModel = viewModel(factory = factory)
    
    // We fetch all entries for the week to draw the grid
    LaunchedEffect(Unit) {
        viewModel.loadAllEntriesForWeek()
    }
    
    var showForm by remember { mutableStateOf(false) }
    var entryToEdit by remember { mutableStateOf<ScheduleEntry?>(null) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    val undoMsg = stringResource(R.string.schedule_undo)
    val deletedMsg = stringResource(R.string.schedule_deleted)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.schedule_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { 
                    entryToEdit = null
                    showForm = true 
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.schedule_add))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            ScheduleGrid(
                entries = viewModel.allEntries,
                onCellClick = { entry ->
                    entryToEdit = entry
                    showForm = true
                }
            )
        }
    }
    
    if (showForm) {
        ScheduleFormDialog(
            initialEntry = entryToEdit,
            accountId = accountId,
            selectedDay = 1, // default to Monday when opened from FAB
            viewModel = viewModel,
            onDismiss = { showForm = false },
            onSave = { entries, isEdit ->
                viewModel.saveEntries(entries, isEdit)
                showForm = false
            },
            onDelete = { entry ->
                viewModel.deleteEntry(entry)
                showForm = false
                scope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = deletedMsg,
                        actionLabel = undoMsg,
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.undoDelete()
                    }
                }
            }
        )
    }
}

val periodTimes = mapOf(
    1 to "07:00\n07:45",
    2 to "07:50\n08:35",
    3 to "08:40\n09:25",
    4 to "09:35\n10:20",
    5 to "10:25\n11:10",
    6 to "13:00\n13:45",
    7 to "13:50\n14:35",
    8 to "14:40\n15:25",
    9 to "15:35\n16:20",
    10 to "16:25\n17:10"
)

@Composable
fun ScheduleGrid(entries: List<ScheduleEntry>, onCellClick: (ScheduleEntry?) -> Unit) {
    val days = listOf("THỨ HAI", "THỨ BA", "THỨ TƯ", "THỨ NĂM", "THỨ SÁU", "THỨ BẢY")
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .horizontalScroll(rememberScrollState())
            .verticalScroll(rememberScrollState())
            .padding(12.dp)
    ) {
        // Header Row
        Row(modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))) {
            Box(modifier = Modifier.width(64.dp).padding(8.dp))
            days.forEach { day ->
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(day, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        
        // Morning header
        Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))) {
            Box(
                modifier = Modifier
                    .width(664.dp) // 64 + 6 * 100
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("SÁNG", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }
        
        // Periods 1-5 (Morning)
        for (period in 1..5) {
            ScheduleRow(period = period, days = 1..6, entries = entries, onClick = onCellClick)
        }

        // Afternoon header
        Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))) {
            Box(
                modifier = Modifier
                    .width(664.dp) // 64 + 6 * 100
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("CHIỀU", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }
        
        // Periods 6-10 (Afternoon)
        for (period in 6..10) {
            val isLast = period == 10
            ScheduleRow(period = period, days = 1..6, entries = entries, onClick = onCellClick, isLast = isLast)
        }
    }
}

@Composable
fun ScheduleRow(period: Int, days: IntRange, entries: List<ScheduleEntry>, onClick: (ScheduleEntry?) -> Unit, isLast: Boolean = false) {
    val bgColor = if (period % 2 == 0) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
    val shape = if (isLast) RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp) else RoundedCornerShape(0.dp)
    Row(modifier = Modifier.background(bgColor, shape)) {
        // Period time column
        Box(
            modifier = Modifier
                .width(64.dp)
                .height(64.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), if (isLast) RoundedCornerShape(bottomStart = 8.dp) else RoundedCornerShape(0.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                periodTimes[period] ?: "",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        
        // Days columns
        days.forEach { dayIdx ->
            val entry = entries.find { it.dayOfWeek == dayIdx && it.startTimeMin == period }
            val cellModifier = Modifier
                .width(100.dp)
                .height(64.dp)
                .clickable { onClick(entry) }
                .padding(4.dp)
            
            Box(
                modifier = cellModifier,
                contentAlignment = Alignment.Center
            ) {
                if (entry != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(2.dp)) {
                            Text(
                                text = entry.title,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}