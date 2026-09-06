package com.example.edu_ai.ui.screens.student

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
                                } else {
                                    viewModel.nextObjective(subtopicId, userId, onTriggerQuiz)
                                }
                            },
                            enabled = !uiState.isLoading,
                            shape = MaterialTheme.shapes.medium,
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Text(if (uiState.userMessage.isNotBlank()) "SEND" else if (uiState.isLast) "FINISH" else "NEXT", fontSize = 12.sp)
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

                FormattedText(
                    text = uiState.content,
                    style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 28.sp),
                    onLinkClick = { activeBrowserUrl = it }
                )
                
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
        
        if (activeBrowserUrl != null) {
            InAppBrowser(
                url = activeBrowserUrl!!,
                onClose = { activeBrowserUrl = null }
            )
        }
    }
}
