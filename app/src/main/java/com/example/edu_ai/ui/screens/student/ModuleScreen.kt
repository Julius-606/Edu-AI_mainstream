
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StickyNote2
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
import com.example.edu_ai.ui.components.FormattedText
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
    
    val tabs = listOf("Chat", "Notes", "Vault", "Quiz")
    val icons = listOf(
        Icons.AutoMirrored.Filled.Chat,
        Icons.Default.StickyNote2,
        Icons.Default.History,
        Icons.Default.LocalFireDepartment
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
                    0 -> ChatTabWrapper(uiState.user, onNavigateToHistory = { selectedTab = 2 })
                    1 -> {
                        if (uiState.user != null) {
                            val notesViewModel: NotesViewModel = viewModel(
                                factory = NotesViewModel.provideFactory(uiState.user!!)
                            )
                            NotesTab(viewModel = notesViewModel)
                        }
                    }
                    2 -> {
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
                    3 -> {
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
                session.description?.let {
                    FormattedText(
                        text = it,
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.secondary),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
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
fun ZenithTab(
    user: UserEntity, 
    progressViewModel: ProgressViewModel,
    timetableViewModel: TimetableViewModel
) {
    val recommendation by progressViewModel.recommendation.collectAsState()
    val progressUiState by progressViewModel.uiState.collectAsState()
    val timetableUiState by timetableViewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        progressViewModel.refreshRecommendations()
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
                    
                    if (progressUiState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        FormattedText(
                            text = recommendation,
                            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Button(
                        onClick = { progressViewModel.refreshRecommendations() },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("REFRESH STRATEGY")
                    }
                }
            }
        }

        item {
            Text("Dynamic Schedule 🗓️", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("AI-optimized study plan", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        }

        if (timetableUiState.isLoading) {
            item {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        } else {
            item {
                AiInsightCard(timetableUiState.aiBrief)
            }

            val groupedByDay = timetableUiState.weeklyPlan.groupBy { it.day }
            groupedByDay.forEach { (day, slots) ->
                item {
                    DayHeader(day)
                }
                items(slots) { slot ->
                    TimetableSlotItem(slot)
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
fun NotesTab(viewModel: NotesViewModel) {
    val notes by viewModel.notes.collectAsState()
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Clinical Notes 📝",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "AI-generated summaries of your consultations.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (notes.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.StickyNote2,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outlineVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No notes generated yet.", color = MaterialTheme.colorScheme.outline)
                    Text("Start a chat to see AI summaries.", style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(notes) { note ->
                    NoteItem(
                        note = note, 
                        onUpdate = { viewModel.updateNote(it) }, 
                        onDelete = { viewModel.deleteNote(note.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun NoteItem(note: com.example.edu_ai.data.local.NoteEntity, onUpdate: (com.example.edu_ai.data.local.NoteEntity) -> Unit, onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var editedContent by remember { mutableStateOf(note.content) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }, 
                horizontalArrangement = Arrangement.SpaceBetween, 
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = note.title, 
                    fontWeight = FontWeight.Bold, 
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Row {
                    IconButton(onClick = { 
                        isEditing = !isEditing 
                        if (isEditing) expanded = true
                    }) {
                        Icon(
                            if (isEditing) Icons.Default.Close else Icons.Default.Edit, 
                            contentDescription = "Edit"
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                    }
                }
            }
            
            androidx.compose.animation.AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    if (isEditing) {
                        OutlinedTextField(
                            value = editedContent,
                            onValueChange = { editedContent = it },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp),
                            label = { Text("Edit Clinical Notes") },
                            shape = MaterialTheme.shapes.medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { 
                                onUpdate(note.copy(content = editedContent))
                                isEditing = false
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Save Changes")
                        }
                    } else {
                        FormattedText(text = note.content)
                    }
                    
                    val date = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(note.lastUpdated))
                    Text(
                        text = "Last updated: $date",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileInfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, color = MaterialTheme.colorScheme.outline)
        Text(text = value, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}


