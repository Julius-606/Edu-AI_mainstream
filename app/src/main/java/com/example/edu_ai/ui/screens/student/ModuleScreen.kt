package com.example.edu_ai.ui.screens.student

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.edu_ai.data.local.ChatSessionEntity
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
    
    val tabs = listOf("Chat", "Vault", "Chaos Quiz", "Progress", "Zenith")
    val icons = listOf(
        Icons.AutoMirrored.Filled.Chat,
        Icons.Default.History,
        Icons.Default.LocalFireDepartment,
        Icons.AutoMirrored.Filled.TrendingUp,
        Icons.Default.AccountCircle
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = tabs[selectedTab],
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        bottomBar = {
            NavigationBar(
                tonalElevation = 8.dp
            ) {
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
                    0 -> ChatTabWrapper(uiState.user, onNavigateToHistory = { selectedTab = 1 })
                    1 -> {
                        if (uiState.user != null) {
                             val chatViewModel: ChatViewModel = viewModel(
                                factory = ChatViewModel.provideFactory(uiState.user!!)
                            )
                             ChatHistoryTab(
                                 viewModel = chatViewModel,
                                 onSessionSelected = { selectedTab = 0 }
                             )
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
                            ZenithTab(user = uiState.user!!, viewModel = progressViewModel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatTabWrapper(user: UserEntity?, onNavigateToHistory: () -> Unit) {
    if (user != null) {
        val chatViewModel: ChatViewModel = viewModel(
            factory = ChatViewModel.provideFactory(user)
        )
        ChatInterface(
            viewModel = chatViewModel,
            onCloseChat = onNavigateToHistory
        )
    }
}

@Composable
fun ChatHistoryTab(
    viewModel: ChatViewModel,
    onSessionSelected: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Consultation Vault 🏦",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Review your previous diagnostic sessions and academic inquiries.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (uiState.archivedSessions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outlineVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No archived consultations found.", color = MaterialTheme.colorScheme.outline)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(uiState.archivedSessions) { session ->
                    SessionItem(
                        session = session,
                        onClick = {
                            viewModel.resumeSession(session)
                            onSessionSelected()
                        },
                        onDelete = { viewModel.deleteSession(session.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun SessionItem(
    session: ChatSessionEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val date = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(session.timestamp))
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Archived on $date",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete Session",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                )
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
fun ZenithTab(user: UserEntity, viewModel: ProgressViewModel) {
    val recommendation by viewModel.recommendation.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshRecommendations()
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        item {
            Column {
                Text("The Zenith Hub 🏔️", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text("Your AI Guide for peak academic performance.", color = MaterialTheme.colorScheme.secondary)
            }
        }
        
        item {
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
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Commander Profile 👤", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ProfileInfoRow(label = "Username", value = user.username)
                        ProfileInfoRow(label = "AI Persona", value = user.aiPersona)
                        ProfileInfoRow(label = "Difficulty", value = user.semesterStatus)
                        ProfileInfoRow(label = "Sensory Mode", value = user.sensoryMode)
                    }
                }
            }
        }

        item {
            Text("Daily Motivation", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "\"The expert in anything was once a beginner.\"",
                    modifier = Modifier.padding(16.dp),
                    fontWeight = FontWeight.Light
                )
            }
        }
        
        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
fun ProfileInfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, color = MaterialTheme.colorScheme.outline)
        Text(text = value, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}
