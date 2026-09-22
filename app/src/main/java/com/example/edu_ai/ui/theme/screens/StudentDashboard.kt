
// #file app/src/main/java/com/eduai/ui/screens/student/StudentDashboard.kt
// #version 1.0.1
// #The main hub for students to see their PnL and launch AI medical cases.

package com.eduai.ui.screens.student

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDashboard(onLogout: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("👩‍🎓 Student Terminal") },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Liquidate Account")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Welcome back, Future Doc!", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("Status: Year 4 - Redemption Arc 🔥", color = MaterialTheme.colorScheme.primary)
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Current PnL (Win Rate)", fontWeight = FontWeight.Bold)
                        Text("📈 78.4% Accuracy in Internal Medicine", fontSize = 18.sp)
                        Text("⚠️ Warning: Stop-loss getting tight in Biochemistry", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            item {
                Button(
                    onClick = { /* TODO: Navigate to Chaos Room! */ },
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                ) {
                    Text("LAUNCH AI CHAOS ENGINE 🧠", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            item {
                Text("Active Contracts (Units)", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }

            // Mock list of active units
            val units = listOf("Biochemistry II", "General Surgery", "Community Health", "Internal Medicine I")
            items(units.size) { index ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "📘 ${units[index]}",
                        modifier = Modifier.padding(16.dp),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

 