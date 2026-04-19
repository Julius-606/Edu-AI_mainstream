package com.example.edu_ai.ui.screens.teacher

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.edu_ai.data.remote.StudentSummary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherDashboard(
    viewModel: TeacherViewModel,
    onLogout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val classReport by viewModel.classReport.collectAsState()
    var editingStudent by remember { mutableStateOf<StudentSummary?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("👨‍🏫 Teacher Portal") },
                actions = {
                    IconButton(onClick = { viewModel.loadDashboard() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.generateClassReport() }) {
                Icon(Icons.Default.Assessment, contentDescription = "Generate AI Report")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val state = uiState) {
                is TeacherUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is TeacherUiState.Success -> {
                    val data = state.data
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Text("Class Overview", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            Text("Monitoring ${data.totalActivePortfolios} Active Portfolios", color = MaterialTheme.colorScheme.secondary)
                        }

                        if (data.riskAlerts.isNotEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("🚨 Risk Management Alerts", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                        data.riskAlerts.forEach { alert ->
                                            Text(alert, fontSize = 14.sp)
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Text("Student Ledger", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }

                        items(data.students) { student ->
                            StudentCard(
                                student = student,
                                onManipulate = { editingStudent = student }
                            )
                        }
                    }
                }
                is TeacherUiState.Error -> {
                    Text(
                        text = "Error: ${state.message}",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center).padding(16.dp)
                    )
                }
            }

            // Report Dialog
            classReport?.let { report ->
                AlertDialog(
                    onDismissRequest = { viewModel.clearReport() },
                    title = { Text("AI Class Performance Report") },
                    text = {
                        Box(modifier = Modifier.heightIn(max = 400.dp)) {
                            LazyColumn {
                                item { Text(report) }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { viewModel.clearReport() }) {
                            Text("Close")
                        }
                    }
                )
            }

            // Manipulation Dialog
            editingStudent?.let { student ->
                ManipulationDialog(
                    student = student,
                    onDismiss = { editingStudent = null },
                    onSave = { units, status ->
                        viewModel.updateStudentPath(student.id.toString(), units, status)
                        editingStudent = null
                    }
                )
            }
        }
    }
}

@Composable
fun StudentCard(student: StudentSummary, onManipulate: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "👤 ${student.username}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                PerformanceBadge(student.averagePnl)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(text = "Status: ${student.semesterStatus}", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
            Text(text = "Quizzes Taken: ${student.totalQuizzes}", fontSize = 14.sp)
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(text = "Study Path: ${student.activeUnits.joinToString(", ")}", 
                fontSize = 13.sp, 
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onManipulate) {
                    Text("MANIPULATE PATH")
                }
            }
        }
    }
}

@Composable
fun ManipulationDialog(
    student: StudentSummary,
    onDismiss: () -> Unit,
    onSave: (List<String>, String) -> Unit
) {
    var unitsText by remember { mutableStateOf(student.activeUnits.joinToString(", ")) }
    var status by remember { mutableStateOf(student.semesterStatus) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manipulate Path: ${student.username}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = status,
                    onValueChange = { status = it },
                    label = { Text("Semester Status / AI Context") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = unitsText,
                    onValueChange = { unitsText = it },
                    label = { Text("Active Units (comma separated)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Note: Directly editing the path bypasses AI automation for these parameters.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val unitsList = unitsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                onSave(unitsList, status)
            }) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun PerformanceBadge(pnl: Double) {
    val color = when {
        pnl >= 80 -> Color(0xFF2E7D32) // Green
        pnl >= 60 -> Color(0xFFF57C00) // Orange
        else -> Color(0xFFD32F2F) // Red
    }
    
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.small,
        border = androidx.compose.foundation.BorderStroke(1.dp, color)
    ) {
        Text(
            text = "${if (pnl >= 60) "Bullish 📈" else "Bearish 📉"} (${pnl}%)",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
