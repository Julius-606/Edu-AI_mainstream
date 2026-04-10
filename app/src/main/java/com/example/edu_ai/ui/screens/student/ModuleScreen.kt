package com.example.edu_ai.ui.screens.student

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModuleScreen(onBack: () -> Unit) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Chat", "History", "Quiz", "Progress", "Zenith")
    
    // Using standard icons that are guaranteed to be in the basic material-icons library
    val icons = listOf(
        Icons.AutoMirrored.Filled.Chat,
        Icons.AutoMirrored.Filled.List,
        Icons.Default.Edit,
        Icons.Default.Star,
        Icons.Default.AccountCircle
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tabs[selectedTab]) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("AI Study Companion", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("Ask anything about your current module...", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ChatHistoryTab() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Previous Consultations", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("Resume or delete your study sessions.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun QuizTab() {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Knowledge Retrieval", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("Test your growth and understanding.", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    }
}
