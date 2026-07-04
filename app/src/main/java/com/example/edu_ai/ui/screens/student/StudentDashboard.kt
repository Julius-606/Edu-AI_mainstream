package com.example.edu_ai.ui.screens.student

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.edu_ai.ui.components.DynamicBackground
import com.example.edu_ai.ui.components.ProgressRings
import com.example.edu_ai.ui.components.RingProgress

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDashboard(
    userId: String,
    onLogout: () -> Unit,
    onLaunchModule: () -> Unit,
    viewModel: StudentViewModel = viewModel(factory = StudentViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Using a simple state for recommendations since it's now integrated
    val progressViewModel: ProgressViewModel = viewModel(factory = ProgressViewModel.provideFactory(uiState.user ?: return Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }))
    val recommendation by progressViewModel.recommendation.collectAsState()
    
    val timetableViewModel: TimetableViewModel = viewModel(factory = TimetableViewModel.provideFactory(uiState.user!!))
    val timetableUiState by timetableViewModel.uiState.collectAsState()

    LaunchedEffect(userId) {
        viewModel.refreshDashboard(userId)
        progressViewModel.refreshRecommendations()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        DynamicBackground()

        Scaffold(
            containerColor = Color.Transparent, // Show background
            topBar = {
                TopAppBar(
                    title = { Text("👩‍🎓 Student Dashboard", fontWeight = FontWeight.ExtraBold) },
                    actions = {
                        IconButton(onClick = onLogout) {
                            Icon(Icons.Default.ExitToApp, contentDescription = "Sign Out")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                    )
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Welcome Section
                item {
                    Column {
                        Text(
                            text = "Welcome back, ${uiState.user?.username ?: "Student"}!",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Status: ${uiState.user?.semesterStatus ?: "Active"}",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                // Progress Rings Section (One per active unit or just the primary one)
                item {
                    Text("Unit Progress Visualization", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }

                items(uiState.units) { unit ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Mock progress data until backend integration is fully wired to ViewModel
                            val rings = listOf(
                                RingProgress(75f, MaterialTheme.colorScheme.primary, "Unit"),
                                RingProgress(60f, MaterialTheme.colorScheme.secondary, "Module"),
                                RingProgress(40f, MaterialTheme.colorScheme.tertiary, "Topic"),
                                RingProgress(20f, Color.Magenta, "Subtopic")
                            )
                            
                            ProgressRings(
                                rings = rings,
                                learningObjectives = listOf("Analyze heart sounds", "Identify JVD", "Measure BP"),
                                modifier = Modifier.size(150.dp)
                            )
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(unit.unitName, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                                Text("Last Studied: Today", style = MaterialTheme.typography.bodySmall)
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(onClick = onLaunchModule) {
                                    Text("CONTINUE")
                                }
                            }
                        }
                    }
                }

                // Zenith Insight Integrated
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Strategic Zenith Insight", fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(recommendation)
                        }
                    }
                }

                // Timetable Preview
                item {
                    Text("Today's Schedule", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }

                val today = java.text.SimpleDateFormat("EEEE", java.util.Locale.getDefault()).format(java.util.Date())
                val todaysSlots = timetableUiState.weeklyPlan.filter { it.day.equals(today, ignoreCase = true) }

                if (todaysSlots.isEmpty()) {
                    item {
                        Text("No sessions scheduled for today. Rest up!", style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    items(todaysSlots) { slot ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text(slot.time, fontWeight = FontWeight.Bold)
                                    Text(slot.activity)
                                }
                                Badge { Text(slot.type) }
                            }
                        }
                    }
                }
                
                item { Spacer(modifier = Modifier.height(40.dp)) }
            }
        }
    }
}
