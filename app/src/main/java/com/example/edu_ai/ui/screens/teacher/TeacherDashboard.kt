package com.example.edu_ai.ui.screens.teacher

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
fun TeacherDashboard(onLogout: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("👨‍🏫 Admin / Teacher Portal") },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Close Terminal")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
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
                Text("Class Overview", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("Monitoring 5 Active Portfolios", color = MaterialTheme.colorScheme.secondary)
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("🚨 Risk Management Alert", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        Text("3 students are currently hitting stop-loss in Anatomy. AI Intervention deployed.")
                    }
                }
            }

            item {
                Text("Student Ledger", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }

            // Mock list of students straight from your Portals.py class list!
            val students = listOf(
                Pair("Neema Ongaga", "Bullish 📈 (89%)"),
                Pair("Grace Naliaka", "Consolidating ➖ (72%)"),
                Pair("Rayvins Otieno", "Bullish 📈 (91%)"),
                Pair("Hillary Lweya", "Bearish 📉 (54%) - Margin Call Risk"),
                Pair("Tatiana A.", "Bullish 📈 (85%)")
            )

            items(students.size) { index ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "👤 ${students[index].first}", fontWeight = FontWeight.Medium)
                        Text(text = students[index].second, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}
