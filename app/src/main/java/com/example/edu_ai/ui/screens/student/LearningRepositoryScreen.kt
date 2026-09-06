package com.example.edu_ai.ui.screens.student

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.edu_ai.EduAIApplication
import com.example.edu_ai.data.local.LearningContentEntity
import com.example.edu_ai.ui.components.DynamicBackground
import com.example.edu_ai.ui.components.FormattedText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearningRepositoryScreen(
    subtopicId: Long,
    subtopicName: String,
    onBack: () -> Unit,
    repository: com.example.edu_ai.repository.EduAIRepository
) {
    val contents by repository.getSavedLearningContent(subtopicId).collectAsState(initial = emptyList())
    var showObjectives by remember { mutableStateOf(true) }

    Box(modifier = Modifier.fillMaxSize()) {
        DynamicBackground()

        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(subtopicName) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { padding ->
            when {
                contents.isEmpty() && showObjectives -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            "Learning Objectives for $subtopicName",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Continue learning to unlock saved content. Here are the key objectives for this subtopic:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "• Understand the core concepts of this topic\n• Apply knowledge through practice exercises\n• Achieve mastery through repeated learning",
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 24.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Button(
                            onClick = { showObjectives = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Got it, Let's Learn")
                        }
                    }
                }
                contents.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                        Text("No content saved for this subtopic yet.", color = MaterialTheme.colorScheme.onSurface)
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        items(contents) { content ->
                            Column {
                                Text(
                                    text = content.objectiveDescription,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                FormattedText(text = content.content)
                                HorizontalDivider(modifier = Modifier.padding(top = 16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
