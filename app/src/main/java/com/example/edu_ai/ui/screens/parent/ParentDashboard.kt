package com.example.edu_ai.ui.screens.parent

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.edu_ai.data.remote.ParentDashboardResponse
import com.example.edu_ai.repository.EduAIRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentDashboard(
    studentId: String,
    repository: EduAIRepository,
    onLogout: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var uiState by remember { mutableStateOf<ParentUiState>(ParentUiState.Loading) }

    fun loadData() {
        uiState = ParentUiState.Loading
        scope.launch {
            try {
                val data = repository.getParentDashboard(studentId)
                uiState = ParentUiState.Success(data)
            } catch (e: Exception) {
                uiState = ParentUiState.Error(e.message ?: "Unknown Error")
            }
        }
    }

    LaunchedEffect(studentId) {
        loadData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("👪 Parent Portal") },
                actions = {
                    IconButton(onClick = { loadData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Sign Out")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val state = uiState) {
                is ParentUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is ParentUiState.Error -> Text("Error: ${state.message}", modifier = Modifier.align(Alignment.Center))
                is ParentUiState.Success -> {
                    val data = state.data
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Text(text = "Viewing Progress for ${data.studentName}", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            Text(text = "Academic Stage: ${data.academicStatus}", color = MaterialTheme.colorScheme.primary)
                        }

                        item {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("🤖 AI Zenith Review", fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(text = data.aiProgressReview)
                                }
                            }
                        }

                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("👨‍🏫 Educator's Remarks", fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(text = data.teacherRemarks ?: "No remarks yet for this period.")
                                }
                            }
                        }

                        item {
                            Text("Current Study Path", fontWeight = FontWeight.Bold)
                            Text(text = data.currentStudyPath.joinToString(" → "), color = MaterialTheme.colorScheme.secondary)
                        }

                        item {
                            Text("Recent Assessments", fontWeight = FontWeight.Bold)
                        }

                        items(data.recentGrades) { grade ->
                            ListItem(
                                headlineContent = { Text(grade.unitName ?: "General") },
                                trailingContent = { Text("${grade.pnl ?: 0}%", fontWeight = FontWeight.Bold) }
                            )
                        }
                    }
                }
            }
        }
    }
}

sealed class ParentUiState {
    object Loading : ParentUiState()
    data class Success(val data: ParentDashboardResponse) : ParentUiState()
    data class Error(val message: String) : ParentUiState()
}
