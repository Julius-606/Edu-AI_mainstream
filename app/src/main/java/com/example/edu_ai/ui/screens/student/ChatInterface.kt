package com.example.edu_ai.ui.screens.student

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.edu_ai.data.remote.ai.ChatMessage
import com.example.edu_ai.ui.components.FormattedText
import kotlinx.coroutines.delay
import java.util.regex.Pattern

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatInterface(
    viewModel: ChatViewModel,
    onCloseChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showHistorySheet by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        // Chat Header - Enhanced with Dynamic Title & History Option
        Surface(
            tonalElevation = 4.dp,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = uiState.currentSessionTitle,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                    Text(
                        text = "The Differential Assistant",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                Row {
                    IconButton(onClick = { showHistorySheet = true }) {
                        Icon(Icons.Default.History, contentDescription = "Chat History", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { viewModel.startNewChat() }) {
                        Icon(Icons.Default.Add, contentDescription = "New Chat", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Chat", tint = MaterialTheme.colorScheme.error)
                    }
                    IconButton(onClick = onCloseChat) {
                        Icon(Icons.Default.Close, contentDescription = "Close Chat")
                    }
                }
            }
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete This Session?") },
                text = { Text("This will permanently remove this consultation from your records.") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.clearChat()
                        showDeleteDialog = false
                    }) {
                        Text("DELETE", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("CANCEL")
                    }
                }
            )
        }

        if (showHistorySheet) {
            ModalBottomSheet(onDismissRequest = { showHistorySheet = false }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text("Past Consultation History", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    if (uiState.archivedSessions.isEmpty()) {
                        Text("No archived consultation sessions.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(uiState.archivedSessions) { session ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.resumeSession(session)
                                            showHistorySheet = false
                                        }
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(session.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                        session.description?.let {
                                            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                        }
                                    }
                                    IconButton(onClick = { viewModel.deleteSession(session.id) }) {
                                        Icon(Icons.Default.Close, contentDescription = "Delete Session", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }

        // 1. Message List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            reverseLayout = false,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            items(uiState.messages) { message ->
                ChatBubble(message)
            }
            if (uiState.isTyping) {
                item {
                    TypingIndicator()
                }
            }
        }

        // 2. Error Display
        uiState.error?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                fontSize = 12.sp
            )
        }

        // 3. Input Area
        Surface(
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Ask about pathology, anatomy...") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            viewModel.sendMessage(inputText)
                            inputText = ""
                        }
                    },
                    enabled = !uiState.isTyping,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun TypingIndicator() {
    var seconds by remember { mutableIntStateOf(0) }
    
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            seconds++
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Timer,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "Analyzing... (${seconds}s)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val isUser = message.role == "user"
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val containerColor = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = alignment) {
        Surface(
            color = containerColor,
            contentColor = contentColor,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            tonalElevation = if (isUser) 0.dp else 1.dp
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                FormattedText(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp)
                )
                
                val youtubeUrl = extractYoutubeUrl(message.content)
                if (youtubeUrl != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    VideoRecommendationCard(url = youtubeUrl)
                }
            }
        }
    }
}

@Composable
fun VideoRecommendationCard(url: String) {
    val context = LocalContext.current
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = Modifier.padding(top = 4.dp),
        onClick = {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        }
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.PlayCircleFilled, contentDescription = null, tint = Color.Red)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Watch Recommended Lesson", style = MaterialTheme.typography.labelLarge)
        }
    }
}

fun extractYoutubeUrl(content: String): String? {
    val ytPattern = "(https?://(?:www\\.)?youtube\\.com/watch\\?v=[\\w-]+|https?://youtu\\.be/[\\w-]+)"
    val pattern = Pattern.compile(ytPattern)
    val matcher = pattern.matcher(content)
    return if (matcher.find()) matcher.group(0) else null
}
