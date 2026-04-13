package com.example.edu_ai.ui.screens.student

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.edu_ai.data.local.UserEntity
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModuleScreen(
    userId: String,
    onBack: () -> Unit,
    studentViewModel: StudentViewModel = viewModel(factory = StudentViewModel.Factory)
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val uiState by studentViewModel.uiState.collectAsState()

    LaunchedEffect(userId) {
        studentViewModel.refreshDashboard(userId)
    }
    
    val tabs = listOf("Chat", "History", "Quiz", "Progress", "Zenith")
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
            if (uiState.isLoading && uiState.user == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                when (selectedTab) {
                    0 -> ChatTabWrapper(uiState.user, onCloseChat = { selectedTab = 1 })
                    1 -> {
                        if (uiState.user != null) {
                             ChatHistoryTab(userId = uiState.user!!.id)
                        }
                    }
                    2 -> {
                        if (uiState.user != null) {
                            val quizViewModel: QuizViewModel = viewModel(
                                factory = QuizViewModel.provideFactory(uiState.user!!)
                            )
                            QuizTabWrapper(
                                user = uiState.user,
                                units = uiState.units,
                                viewModel = quizViewModel
                            )
                        }
                    }
                    3 -> {
                        if (uiState.user != null) {
                            val progressViewModel: ProgressViewModel = viewModel(
                                factory = ProgressViewModel.provideFactory(uiState.user!!)
                            )
                            ProgressTab(viewModel = progressViewModel)
                        }
                    }
                    4 -> {
                        if (uiState.user != null) {
                            val progressViewModel: ProgressViewModel = viewModel(
                                factory = ProgressViewModel.provideFactory(uiState.user!!)
                            )
                            ZenithTab(viewModel = progressViewModel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatTabWrapper(user: UserEntity?, onCloseChat: () -> Unit) {
    var isChatStarted by remember { mutableStateOf(false) }

    if (!isChatStarted) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("AI Study Companion", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Ask anything about your current module...", color = MaterialTheme.colorScheme.onSurfaceVariant)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = { isChatStarted = true },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("START 🚀", fontWeight = FontWeight.Bold)
            }
        }
    } else {
        if (user != null) {
            val chatViewModel: ChatViewModel = viewModel(
                factory = ChatViewModel.provideFactory(user)
            )
            ChatInterface(
                viewModel = chatViewModel,
                onCloseChat = onCloseChat
            )
        }
    }
}

@Composable
fun ChatHistoryTab(userId: String) {
    // For now simple listing of messages as a session history placeholder
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Previous Consultations", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("Review your study sessions with the AI.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Session: General Inquiry", fontWeight = FontWeight.Bold)
                Text("Click to view full transcript (Feature coming soon)", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun ProgressTab(viewModel: ProgressViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Learning Analytics", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Monitoring your progressive growth", color = MaterialTheme.colorScheme.primary)
        }

        item {
            Text("Recent Portfolio Performance", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        if (uiState.quizHistory.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No quiz data yet. Start an assessment to see results!", color = Color.Gray)
                }
            }
        }

        items(uiState.quizHistory) { history ->
            val date = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(history.timestamp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { /* Logic to open specific quiz for review */ }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(history.unitName, fontWeight = FontWeight.Bold)
                        Text(date, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                    Text(
                        "${history.pnlScore.toInt()}%",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (history.pnlScore >= 70) Color(0xFF4CAF50) else Color(0xFFF44336)
                    )
                }
            }
        }
    }
}

@Composable
fun ZenithTab(viewModel: ProgressViewModel) {
    val recommendation by viewModel.recommendation.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshRecommendations()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("The Zenith Hub 🏔️", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Your AI Guide for peak academic performance.", color = MaterialTheme.colorScheme.secondary)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Strategic Guidance", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Text(
                        text = recommendation,
                        style = MaterialTheme.typography.bodyLarge,
                        lineHeight = 24.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Button(
                    onClick = { viewModel.refreshRecommendations() },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("REFRESH STRATEGY")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        Text("Daily Motivation", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Text(
                "\"The expert in anything was once a beginner.\"",
                modifier = Modifier.padding(16.dp),
                fontWeight = FontWeight.Light
            )
        }
    }
}
