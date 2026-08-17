package com.example.edu_ai.ui.screens.student

import androidx.compose.foundation.clickable
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
import com.example.edu_ai.data.local.UnitWithModules
import com.example.edu_ai.ui.components.DynamicBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitOutlineScreen(
    unitId: Long,
    onBack: () -> Unit,
    onViewSubtopicRepository: (Long, String) -> Unit,
    viewModel: StudentViewModel = viewModel(factory = StudentViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val unitWithModules = uiState.unitsWithModules.find { it.unit.localId == unitId }

    Box(modifier = Modifier.fillMaxSize()) {
        DynamicBackground()

        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(unitWithModules?.unit?.unitName ?: "Unit Outline") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { padding ->
            if (unitWithModules == null) {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Text("Unit not found")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    unitWithModules.modules.forEach { moduleWithTopics ->
                        item {
                            Text(
                                text = "Module: ${moduleWithTopics.module.name}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        moduleWithTopics.topics.forEach { topicWithSubtopics ->
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "Topic: ${topicWithSubtopics.topic.name}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        topicWithSubtopics.subtopics.forEach { subtopic ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { onViewSubtopicRepository(subtopic.subtopicId, subtopic.name) }
                                                    .padding(start = 8.dp, top = 4.dp, bottom = 4.dp),
                                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                            ) {
                                                val statusColor = if (subtopic.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                                Surface(
                                                    modifier = Modifier.size(8.dp),
                                                    shape = androidx.compose.foundation.shape.CircleShape,
                                                    color = statusColor
                                                ) {}
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = subtopic.name,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = if (subtopic.isCompleted) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
