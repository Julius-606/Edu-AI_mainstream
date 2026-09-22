
package com.example.edu_ai.ui.screens.teacher

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.edu_ai.data.remote.StudentSummary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherDashboard(
    viewModel: TeacherViewModel,
    onLogout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val classReport by viewModel.classReport.collectAsState()
    var editingStudent by remember { mutableStateOf<StudentSummary?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("👨‍🏫 Educator Command Center") },
                actions = {
                    IconButton(onClick = { viewModel.loadDashboard() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Data")
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Sign Out")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.generateClassReport() }) {
                Icon(Icons.Default.AutoAwesome, contentDescription = "AI Performance Analysis")
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
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Text("Action Required Queue", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            Text("Priority focus on ${data.actionRequiredQueue.size} students requiring intervention.", 
                                color = MaterialTheme.colorScheme.secondary)
                        }

                        if (data.actionRequiredQueue.isEmpty()) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                    Text("All students are currently meeting learning milestones. ✨", color = Color.Gray)
                                }
                            }
                        } else {
                            items(data.actionRequiredQueue) { student ->
                                ActionRequiredCard(
                                    student = student,
                                    onAdjust = { editingStudent = student },
                                    onSendReport = {
                                        scope.launch {
                                            viewModel.sendProgressReport(student.id.toString())
                                            snackbarHostState.showSnackbar("Progress report routed to Parent Portal for ${student.username}")
                                        }
                                    }
                                )
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Divider()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Class Health Score: ${data.classHealthScore}%", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                is TeacherUiState.Error -> {
                    Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.Center))
                }
            }

            // Analysis Modal
            classReport?.let { report ->
                AlertDialog(
                    onDismissRequest = { viewModel.clearReport() },
                    title = { Text("AI Educator Insights") },
                    text = {
                        Box(modifier = Modifier.heightIn(max = 400.dp)) {
                            LazyColumn { item { Text(report) } }
                        }
                    },
                    confirmButton = { TextButton(onClick = { viewModel.clearReport() }) { Text("Acknowledge") } }
                )
            }

            // Adjust Plan Modal
            editingStudent?.let { student ->
                AdjustmentModal(
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
fun ActionRequiredCard(
    student: StudentSummary, 
    onAdjust: () -> Unit,
    onSendReport: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(text = student.username, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(text = "${student.averagePnl}%", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            }
            
            Text(text = student.riskReason ?: "Deviating from learning path", 
                color = MaterialTheme.colorScheme.error, 
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium)
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onAdjust, modifier = Modifier.weight(1f)) {
                    Text("Adjust Plan", fontSize = 12.sp)
                }
                OutlinedButton(onClick = onSendReport, modifier = Modifier.weight(1f)) {
                    Text("Route to Parent", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun AdjustmentModal(
    student: StudentSummary,
    onDismiss: () -> Unit,
    onSave: (List<String>, String) -> Unit
) {
    var unitsText by remember { mutableStateOf(student.activeUnits.joinToString(", ")) }
    var status by remember { mutableStateOf(student.semesterStatus) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Intervention: ${student.username}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = status, onValueChange = { status = it }, label = { Text("Learning Stage") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = unitsText, onValueChange = { unitsText = it }, label = { Text("Core Units") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = {
                val unitsList = unitsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                onSave(unitsList, status)
            }) { Text("Deploy Update") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}


