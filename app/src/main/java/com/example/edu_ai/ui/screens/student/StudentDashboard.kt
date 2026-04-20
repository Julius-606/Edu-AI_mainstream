package com.example.edu_ai.ui.screens.student

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDashboard(
    userId: String,
    onLogout: () -> Unit,
    onLaunchModule: () -> Unit,
    viewModel: StudentViewModel = viewModel(factory = StudentViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()

    // Refresh data using the real User ID passed from login
    LaunchedEffect(userId) {
        viewModel.refreshDashboard(userId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("👩‍🎓 Student Dashboard") },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Sign Out")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        if (uiState.isLoading && uiState.user == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                uiState.error?.let { error ->
                    item {
                        Text(text = "Error: $error", color = MaterialTheme.colorScheme.error)
                    }
                }

                item {
                    Text(
                        text = "Welcome back, ${uiState.user?.username ?: "Student"}!",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Status: ${uiState.user?.semesterStatus ?: "Active"}",
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Learning Insights", fontWeight = FontWeight.Bold)
                            Text("🧠 AI Companion: ${uiState.user?.aiPersona ?: "N/A"}", fontSize = 18.sp)
                            Text("Study Mode: ${uiState.user?.sensoryMode ?: "N/A"}", color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }

                item {
                    Button(
                        onClick = onLaunchModule,
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        Text("START LEARNING 📚", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                item {
                    Text("Active Units", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }

                items(uiState.units) { unit ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "📘 ${unit.unitName}",
                            modifier = Modifier.padding(16.dp),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
