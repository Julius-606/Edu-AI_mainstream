package com.example.edu_ai.ui.screens.student

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.edu_ai.data.local.ModuleEntity
import com.example.edu_ai.data.local.ModuleWithTopics
import com.example.edu_ai.data.local.SubtopicEntity
import com.example.edu_ai.data.local.TopicEntity
import com.example.edu_ai.data.local.TopicWithSubtopics
import com.example.edu_ai.data.local.UnitEntity
import com.example.edu_ai.data.local.UnitWithModules
import com.example.edu_ai.data.local.UserEntity
import com.example.edu_ai.data.remote.ApiTimetableSlot
import com.example.edu_ai.ui.components.DynamicBackground
import com.example.edu_ai.ui.components.ProgressRings
import com.example.edu_ai.ui.components.RingProgress
import com.example.edu_ai.ui.theme.TraceTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDashboard(
    userId: String,
    onLogout: () -> Unit,
    onLaunchModule: (Int?) -> Unit,
    onOpenLibrary: () -> Unit,
    onViewUnitOutline: (Long) -> Unit,
    onOpenConsultations: () -> Unit,
    viewModel: StudentViewModel = viewModel(factory = StudentViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Use the userId directly if user entity is not yet loaded in DAO
    val user = uiState.user ?: UserEntity(id = userId, username = "Student", role = "student", difficulty = "Medium (Standard)", semesterStatus = "Active", aiPersona = "Helper")

    val progressViewModel: ProgressViewModel = viewModel(factory = ProgressViewModel.provideFactory(user))
    val recommendation by progressViewModel.recommendation.collectAsState()
    
    val timetableViewModel: TimetableViewModel = viewModel(factory = TimetableViewModel.provideFactory(user))
    val timetableUiState by timetableViewModel.uiState.collectAsState()

    LaunchedEffect(userId) {
        viewModel.refreshDashboard(userId)
        progressViewModel.refreshRecommendations()
    }

    StudentDashboardContent(
        user = user,
        uiState = uiState,
        recommendation = recommendation,
        timetableUiState = timetableUiState,
        onLogout = onLogout,
        onLaunchModule = onLaunchModule,
        onOpenLibrary = onOpenLibrary,
        onViewUnitOutline = onViewUnitOutline,
        onOpenConsultations = onOpenConsultations
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDashboardContent(
    user: UserEntity,
    uiState: StudentUiState,
    recommendation: String,
    timetableUiState: TimetableUiState,
    onLogout: () -> Unit,
    onLaunchModule: (Int?) -> Unit,
    onOpenLibrary: () -> Unit,
    onViewUnitOutline: (Long) -> Unit,
    onOpenConsultations: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        DynamicBackground()

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { 
                        Column {
                            Text("Trace Portal", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                            Text("Student Dashboard", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    },
                    actions = {
                        IconButton(onClick = onOpenConsultations) {
                            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Consultations", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = onLogout) {
                            Icon(Icons.Default.ExitToApp, contentDescription = "Sign Out")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                    )
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }

                // Welcome Section
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Hi, ${user.username}!",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-1).sp
                            )
                            Text(
                                text = user.semesterStatus,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                             FilledTonalButton(
                                onClick = onOpenLibrary,
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("UNIT")
                            }
                        }
                    }
                }

                // Zenith Insight (Prominent)
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                        ),
                        shape = MaterialTheme.shapes.extraLarge
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.AutoAwesome, 
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("ZENITH INSIGHT", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                recommendation,
                                style = MaterialTheme.typography.bodyLarge,
                                lineHeight = 22.sp
                            )
                        }
                    }
                }

                item {
                    Text("Current Progress", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                if (uiState.unitsWithModules.isEmpty() && !uiState.isLoading) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "Your library is empty.", 
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(onClick = onOpenLibrary) {
                                    Text("ADD YOUR FIRST UNIT")
                                }
                            }
                        }
                    }
                }

                // Hierarchical Progress Cards
                items(uiState.unitsWithModules) { unitWithModules ->
                    val unit = unitWithModules.unit
                    
                    // Calculate hierarchical progress
                    val allTopics = unitWithModules.modules.flatMap { it.topics }
                    val allSubtopics = allTopics.flatMap { it.subtopics }
                    
                    val unitProgress = if (allSubtopics.isNotEmpty()) {
                        (allSubtopics.count { it.isCompleted }.toFloat() / allSubtopics.size) * 100f
                    } else 0f

                    // Example: Current Module (First one)
                    val currentModule = unitWithModules.modules.firstOrNull()
                    val currentModuleSubtopics = currentModule?.topics?.flatMap { it.subtopics } ?: emptyList()
                    
                    val moduleProgress = if (currentModuleSubtopics.isNotEmpty()) {
                        (currentModuleSubtopics.count { it.isCompleted }.toFloat() / currentModuleSubtopics.size) * 100f
                    } else 0f
                    
                    val currentSubtopic = allSubtopics.find { !it.isCompleted } ?: allSubtopics.lastOrNull()

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val rings = listOf(
                                RingProgress(unitProgress, MaterialTheme.colorScheme.primary, "Unit"),
                                RingProgress(moduleProgress, MaterialTheme.colorScheme.secondary, "Module"),
                                RingProgress(if (currentSubtopic?.isCompleted == true) 100f else 0f, MaterialTheme.colorScheme.tertiary, "Subtopic")
                            )
                            
                            ProgressRings(
                                rings = rings,
                                learningObjectives = emptyList(), // Removed FOCUS list
                                modifier = Modifier.size(120.dp)
                            )
                            
                            Spacer(modifier = Modifier.width(20.dp))
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    unit.unitName, 
                                    fontWeight = FontWeight.ExtraBold, 
                                    fontSize = 20.sp,
                                    lineHeight = 24.sp,
                                    modifier = Modifier.clickable { onViewUnitOutline(unit.localId) }
                                )
                                Text(
                                    currentSubtopic?.name ?: "Unit Completed", 
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    currentModule?.module?.name ?: "No Active Module", 
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { 
                                        val subtopicToStart = allSubtopics.find { !it.isCompleted } ?: allSubtopics.firstOrNull()
                                        onLaunchModule(subtopicToStart?.subtopicId?.toInt())
                                    },
                                    shape = MaterialTheme.shapes.medium,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("CONTINUE")
                                }
                            }
                        }
                    }
                }

                // Timetable Preview
                item {
                    Text("Today's Schedule", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                val today = java.text.SimpleDateFormat("EEEE", java.util.Locale.getDefault()).format(java.util.Date())
                val todaysSlots = timetableUiState.weeklyPlan.filter { it.day.equals(today, ignoreCase = true) }

                if (todaysSlots.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text(
                                "No sessions scheduled for today. Rest up!", 
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    items(todaysSlots) { slot ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp), 
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(slot.time, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Text(slot.activity, style = MaterialTheme.typography.bodyLarge)
                                }
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text(slot.type) }
                                )
                            }
                        }
                    }
                }

                // Full Weekly Timetable
                item {
                    Text("Weekly Schedule", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                if (timetableUiState.weeklyPlan.isNotEmpty()) {
                    val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
                    items(days) { day ->
                        val slotsForDay = timetableUiState.weeklyPlan.filter { it.day.equals(day, ignoreCase = true) }
                        if (slotsForDay.isNotEmpty()) {
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Text(day, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                slotsForDay.forEach { slot ->
                                    Surface(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                        shape = MaterialTheme.shapes.small
                                    ) {
                                        Row(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("${slot.time}: ${slot.activity}", style = MaterialTheme.typography.bodySmall)
                                            Text(slot.type, style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                item { Spacer(modifier = Modifier.height(40.dp)) }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun StudentDashboardPreview() {
    val sampleUser = UserEntity(
        id = "1",
        username = "Jay",
        role = "student",
        difficulty = "Medium (Standard)",
        semesterStatus = "Year 4 - Semester 2",
        aiPersona = "Motivator"
    )

    val sampleUnitsWithModules = listOf(
        UnitWithModules(
            unit = UnitEntity(localId = 1L, unitName = "Data Science", isActive = true),
            modules = listOf(
                ModuleWithTopics(
                    module = ModuleEntity(moduleId = 1L, unitId = 1L, name = "Introduction to ML"),
                    topics = listOf(
                        TopicWithSubtopics(
                            topic = TopicEntity(topicId = 1L, moduleId = 1L, name = "Supervised Learning"),
                            subtopics = listOf(
                                SubtopicEntity(subtopicId = 1L, topicId = 1L, name = "Regression", isCompleted = true),
                                SubtopicEntity(subtopicId = 2L, topicId = 1L, name = "Classification", isCompleted = false)
                            )
                        )
                    )
                )
            )
        )
    )

    val sampleStudentUiState = StudentUiState(
        user = sampleUser,
        unitsWithModules = sampleUnitsWithModules
    )

    val sampleTimetableUiState = TimetableUiState(
        weeklyPlan = listOf(
            ApiTimetableSlot(
                day = java.text.SimpleDateFormat("EEEE", java.util.Locale.getDefault()).format(java.util.Date()),
                time = "10:00 AM",
                activity = "ML Lecture",
                unit = "Data Science",
                type = "Lecture"
            )
        )
    )

    TraceTheme {
        StudentDashboardContent(
            user = sampleUser,
            uiState = sampleStudentUiState,
            recommendation = "Keep up the great work! You're making steady progress in Data Science.",
            timetableUiState = sampleTimetableUiState,
            onLogout = {},
            onLaunchModule = {},
            onOpenLibrary = {},
            onViewUnitOutline = {},
            onOpenConsultations = {}
        )
    }
}
