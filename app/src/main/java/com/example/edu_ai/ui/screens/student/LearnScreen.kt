package com.example.edu_ai.ui.screens.student

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.edu_ai.EduAIApplication
import com.example.edu_ai.ui.components.DynamicBackground

import com.example.edu_ai.ui.components.FormattedText
import com.example.edu_ai.ui.components.InAppBrowser
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearnScreen(
    subtopicId: Int,
    userId: String,
    onBack: () -> Unit,
    onTriggerQuiz: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as EduAIApplication
    val viewModel: LearnViewModel = viewModel(
        factory = LearnViewModel.Factory(app.repository)
    )
    val uiState by viewModel.uiState.collectAsState()
    
    var activeBrowserUrl by remember { mutableStateOf<String?>(null) }
    var showSavedPanel by remember { mutableStateOf(false) }
    val bookmarks by app.database.dao().getBookmarks(userId).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    LaunchedEffect(subtopicId) {
        viewModel.loadSession(subtopicId, userId)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        DynamicBackground()

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(uiState.subtopicName, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("Learning Trace", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            scope.launch {
                                app.database.dao().insertBookmark(
                                    com.example.edu_ai.data.local.BookmarkEntity(
                                        userId = userId,
                                        subtopicId = subtopicId.toLong(),
                                        objectiveDescription = uiState.objectiveDescription,
                                        excerpt = uiState.content.take(180)
                                    )
                                )
                            }
                        }) {
                            Icon(Icons.Default.Bookmark, contentDescription = "Bookmark this point")
                        }
                        IconButton(onClick = { showSavedPanel = true }) {
                            Icon(Icons.Default.NoteAdd, contentDescription = "Bookmarks and notes")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                    )
                )
            },
            bottomBar = {
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    tonalElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.previousObjective(subtopicId, userId) },
                            enabled = !uiState.isFirst && !uiState.isLoading
                        ) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Back")
                        }

                        OutlinedTextField(
                            value = uiState.userMessage,
                            onValueChange = { viewModel.onUserMessageChange(it) },
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                            placeholder = { Text("Ask AI...", style = MaterialTheme.typography.bodySmall) },
                            maxLines = 2,
                            shape = MaterialTheme.shapes.medium,
                            textStyle = MaterialTheme.typography.bodySmall
                        )

                        Button(
                            onClick = {
                                if (uiState.userMessage.isNotBlank()) {
                                    viewModel.sendUserQuestion(subtopicId, userId)
                                } else if (uiState.interactionSatisfied) {
                                    viewModel.nextObjective(subtopicId, userId, onTriggerQuiz)
                                } else {
                                    viewModel.nextObjective(subtopicId, userId, onTriggerQuiz)
                                }
                            },
                            enabled = !uiState.isLoading,
                            shape = MaterialTheme.shapes.medium,
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Text(if (uiState.userMessage.isNotBlank()) "SEND" else if (uiState.interactionSatisfied && uiState.isLast) "FINISH" else "NEXT", fontSize = 12.sp)
                            Icon(if (uiState.userMessage.isNotBlank()) Icons.Default.ChevronRight else Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                if (uiState.isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Text(
                    text = uiState.objectiveDescription,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(24.dp))

                SelectionContainer {
                    FormattedText(
                        text = uiState.content,
                        style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 28.sp),
                        onLinkClick = { activeBrowserUrl = it }
                    )
                }
                uiState.interactionResponse?.let {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                        Text(it, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
                    }
                }
                
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
        
        if (activeBrowserUrl != null) {
            InAppBrowser(
                url = activeBrowserUrl!!,
                onClose = { activeBrowserUrl = null }
            )
        }

        if (showSavedPanel) {
            ModalBottomSheet(onDismissRequest = { showSavedPanel = false }) {
                Column(Modifier.fillMaxWidth().padding(20.dp)) {
                    Text("Trace saves", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Bookmarks and notes from your learning trail", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = {
                        scope.launch {
                            app.database.dao().insertNote(
                                com.example.edu_ai.data.local.NoteEntity(
                                    userId = userId,
                                    sessionId = 0,
                                    title = uiState.objectiveDescription,
                                    content = uiState.content
                                )
                            )
                        }
                    }) { Text("Save current content as note") }
                    LazyColumn {
                        items(bookmarks) { bookmark ->
                            ListItem(
                                headlineContent = { Text(bookmark.objectiveDescription) },
                                supportingContent = { Text(bookmark.excerpt) },
                                trailingContent = {
                                    IconButton(onClick = { scope.launch { app.database.dao().deleteBookmark(bookmark.id) } }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete bookmark")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
