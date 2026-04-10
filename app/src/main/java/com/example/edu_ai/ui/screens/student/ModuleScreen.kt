package com.example.edu_ai.ui.screens.student

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ModuleScreen(onBack: () -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Chat", "History", "Quiz", "Progress", "Zenith")
    val icons = listOf(
        Icons.Default.Chat,
        Icons.Default.History,
        Icons.Default.Quiz,
        Icons.Default.Assessment,
        Icons.Default.AutoAwesome // "Zenith" - Motivation & Settings
    )

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text(tabs[selectedTab]) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, title ->
                    NavigationBarItem(
                        icon = { Icon(icons[index], contentDescription = title) },
                        label = { Text(title) },
                        selected = selectedTab == index,
                        onClick = { selectedTab = index }
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (selectedTab) {
                0 -> ChatTab()
                1 -> ChatHistoryTab()
                2 -> QuizTab()
                3 -> ProgressTab()
                4 -> ZenithTab()
            }
        }
    }
}

@Composable
fun ChatTab() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("AI Study Companion", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("Ask anything about your current module...")
        // Gemini Integration will go here
    }
}

@Composable
fun ChatHistoryTab() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Previous Consultations", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("Resume or delete your study sessions.")
        // List of history items
    }
}

@Composable
fun QuizTab() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Knowledge Retrieval", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Button(onClick = { /* Start Quiz */ }, modifier = Modifier.padding(top = 16.dp)) {
            Text("START ASSESSMENT")
        }
    }
}

@Composable
fun ProgressTab() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Learning Analytics", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("Personalized Growth Tracking", color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("AI Recommendation:", fontWeight = FontWeight.Bold)
                Text("Focus on 'Cellular Respiration' in Biochemistry. Take this topic to Chat for clarification.")
            }
        }
    }
}

@Composable
fun ZenithTab() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("The Zenith Hub", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("Settings & Daily Motivation", color = MaterialTheme.colorScheme.secondary)
        Spacer(modifier = Modifier.height(24.dp))
        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Text(
                "\"The expert in anything was once a beginner.\"",
                modifier = Modifier.padding(16.dp),
                fontWeight = FontWeight.Light
            )
        }
        // Basic settings could go here
    }
}
