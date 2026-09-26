package com.example.edu_ai.ui.screens.student

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.edu_ai.data.local.ModuleWithTopics
import com.example.edu_ai.data.local.QuizHistoryEntity
import com.example.edu_ai.data.local.UnitEntity
import com.example.edu_ai.data.local.UnitWithModules
import com.example.edu_ai.ui.components.FormattedText
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun QuizTabWrapper(
    user: com.example.edu_ai.data.local.UserEntity?,
    units: List<UnitEntity>,
    viewModel: QuizViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.quiz == null) {
        UnitSelectionScreen(
            unitsWithModules = uiState.unitsWithModules,
            history = uiState.quizHistory,
            selectedUnit = uiState.selectedUnit,
            onUnitClicked = { viewModel.selectUnit(it) },
            onStartQuiz = { unit, topic -> viewModel.startQuiz(unit, topic) },
            isLoading = uiState.isLoading,
            error = uiState.error
        )
    } else if (uiState.isQuizFinished) {
        QuizResultScreen(
            score = uiState.score,
            total = uiState.quiz?.questions?.size ?: 0,
            unitName = uiState.selectedTopic ?: uiState.quiz?.title ?: "Assessment",
            onReview = { viewModel.enterReviewMode() },
            onRetake = { viewModel.startQuiz(uiState.selectedUnit ?: "", uiState.selectedTopic) },
            onTakeNew = { viewModel.resetQuizSelection() },
            onGoBack = { viewModel.resetQuizSelection() }
        )
    } else {
        QuizQuestionScreen(
            viewModel = viewModel,
            uiState = uiState
        )
    }
}

@Composable
fun UnitSelectionScreen(
    unitsWithModules: List<UnitWithModules>,
    history: List<QuizHistoryEntity>,
    selectedUnit: String?,
    onUnitClicked: (String) -> Unit,
    onStartQuiz: (String, String?) -> Unit,
    isLoading: Boolean,
    error: String?
) {
    var focusArea by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Knowledge Retrieval Center", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
        Text("Select a syllabus unit or subtopic. Take new assessments or review retake performance.", fontSize = 11.sp, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        
        Spacer(modifier = Modifier.height(14.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("AI is synthesizing your clinical assessment...", style = MaterialTheme.typography.bodySmall, fontSize = 12.sp)
                }
            }
        } else {
            error?.let {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Text(it, color = MaterialTheme.colorScheme.error, fontSize = 11.sp, modifier = Modifier.padding(10.dp))
                }
            }
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.School, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Active Syllabus Units", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                items(unitsWithModules) { unitWithModules ->
                    val unit = unitWithModules.unit
                    val isSelected = selectedUnit == unit.unitName
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onUnitClicked(unit.unitName) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text(unit.unitName, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                Icon(
                                    imageVector = if (isSelected) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            AnimatedVisibility(visible = isSelected) {
                                Column {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    unitWithModules.modules.forEach { moduleWithTopics ->
                                        ModuleAccordion(
                                            unitName = unit.unitName,
                                            moduleWithTopics = moduleWithTopics,
                                            history = history,
                                            onSubtopicClicked = { onStartQuiz(unit.unitName, it) }
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(12.dp))
                                    OutlinedTextField(
                                        value = focusArea,
                                        onValueChange = { focusArea = it },
                                        placeholder = { Text("Specific area of focus (e.g. ECG Analysis, Pharmacology)", fontSize = 11.sp) },
                                        modifier = Modifier.fillMaxWidth(),
                                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = { onStartQuiz(unit.unitName, focusArea.ifBlank { null }) },
                                        modifier = Modifier.fillMaxWidth().height(42.dp),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("GENERATE UNIT ASSESSMENT", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    // Distinguish Assessment History & Retakes inside dropdown
                                    val unitSubtopics = unitWithModules.modules.flatMap { it.topics }.flatMap { it.subtopics }.map { it.name }
                                    val unitHistory = history.filter { record ->
                                        record.unitName.equals(unit.unitName, ignoreCase = true) ||
                                        unitSubtopics.any { subName -> subName.equals(record.unitName, ignoreCase = true) }
                                    }

                                    if (unitHistory.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                        Spacer(modifier = Modifier.height(10.dp))
                                        
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Assessment Records & Retakes", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Group by target name to distinguish initial attempts from retakes
                                        val groupedHistory = unitHistory.groupBy { it.unitName }

                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            groupedHistory.forEach { (topicName, attempts) ->
                                                // Sort chronologically ascending to assign Attempt #1, #2, etc.
                                                val sortedAttempts = attempts.sortedBy { it.timestamp }
                                                
                                                Card(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
                                                    shape = RoundedCornerShape(10.dp),
                                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                                ) {
                                                    Column(modifier = Modifier.padding(12.dp)) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(topicName, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                                            
                                                            // Quick Retake Button
                                                            FilledTonalButton(
                                                                onClick = { onStartQuiz(unit.unitName, topicName) },
                                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                                modifier = Modifier.height(28.dp),
                                                                shape = RoundedCornerShape(6.dp)
                                                            ) {
                                                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(12.dp))
                                                                Spacer(modifier = Modifier.width(3.dp))
                                                                Text("RETAKE", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                            }
                                                        }

                                                        Spacer(modifier = Modifier.height(8.dp))

                                                        // List of attempts with clear badges and delta improvement
                                                        sortedAttempts.forEachIndexed { attemptIndex, attempt ->
                                                            val isInitial = attemptIndex == 0
                                                            val attemptLabel = if (isInitial) "Attempt #1 (Initial)" else "Attempt #${attemptIndex + 1} (Retake)"
                                                            val scoreColor = if (attempt.pnlScore >= 70) Color(0xFF10B981) else Color(0xFFEF4444)
                                                            
                                                            val prevScore = if (attemptIndex > 0) sortedAttempts[attemptIndex - 1].pnlScore else null
                                                            val delta = if (prevScore != null) attempt.pnlScore - prevScore else null

                                                            Row(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .padding(vertical = 3.dp),
                                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                                    Surface(
                                                                        shape = RoundedCornerShape(4.dp),
                                                                        color = if (isInitial) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color(0xFFF59E0B).copy(alpha = 0.15f)
                                                                    ) {
                                                                        Text(
                                                                            text = attemptLabel,
                                                                            fontSize = 9.sp,
                                                                            fontWeight = FontWeight.Bold,
                                                                            color = if (isInitial) MaterialTheme.colorScheme.primary else Color(0xFFF59E0B),
                                                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                                        )
                                                                    }
                                                                    Spacer(modifier = Modifier.width(6.dp))
                                                                    Text(
                                                                        SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(attempt.timestamp)),
                                                                        fontSize = 9.sp,
                                                                        color = MaterialTheme.colorScheme.outline
                                                                    )
                                                                }

                                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                                    if (delta != null) {
                                                                        val isGain = delta >= 0
                                                                        Text(
                                                                            text = if (isGain) "+${delta.toInt()}% ↗" else "${delta.toInt()}% ↘",
                                                                            fontSize = 9.sp,
                                                                            fontWeight = FontWeight.Bold,
                                                                            color = if (isGain) Color(0xFF10B981) else Color(0xFFEF4444),
                                                                            modifier = Modifier.padding(end = 6.dp)
                                                                        )
                                                                    }
                                                                    Text(
                                                                        "${attempt.pnlScore.toInt()}%",
                                                                        fontWeight = FontWeight.ExtraBold,
                                                                        fontSize = 12.sp,
                                                                        color = scoreColor
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
                }
            }
        }
    }
}

@Composable
fun ModuleAccordion(
    unitName: String,
    moduleWithTopics: ModuleWithTopics,
    history: List<QuizHistoryEntity>,
    onSubtopicClicked: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = Modifier.padding(vertical = 3.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                moduleWithTopics.module.name,
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        if (expanded) {
            moduleWithTopics.topics.flatMap { it.subtopics }.forEach { subtopic ->
                val subtopicHistory = history.filter { it.unitName.equals(subtopic.name, ignoreCase = true) }
                    .sortedBy { it.timestamp }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, top = 5.dp, bottom = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f).clickable { onSubtopicClicked(subtopic.name) }
                    ) {
                        Icon(
                            imageVector = if (subtopic.isCompleted) Icons.Default.CheckCircle else Icons.Default.School,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = if (subtopic.isCompleted) Color(0xFF10B981) else MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            subtopic.name,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    // Distinguish initial attempts vs retakes on subtopic bubbles
                    if (subtopicHistory.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(start = 6.dp)
                        ) {
                            subtopicHistory.forEachIndexed { idx, attempt ->
                                val isInitial = idx == 0
                                val bubbleColor = if (attempt.pnlScore >= 70) Color(0xFF10B981) else Color(0xFFEF4444)
                                val label = if (isInitial) "1st: ${attempt.pnlScore.toInt()}%" else "Retake #${idx}: ${attempt.pnlScore.toInt()}%"
                                
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = bubbleColor.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, bubbleColor.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = bubbleColor,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
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

@Composable
fun QuizQuestionScreen(
    viewModel: QuizViewModel,
    uiState: QuizUiState
) {
    val quiz = uiState.quiz ?: return
    val currentIdx = uiState.currentQuestionIndex
    val currentQuestion = quiz.questions.getOrNull(currentIdx) ?: return
    val selectedOption = uiState.selectedOptions[currentIdx]
    val isSubmitted = uiState.submittedQuestions.contains(currentIdx)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            // Header Progress Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = uiState.selectedTopic ?: quiz.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Q ${currentIdx + 1}/${quiz.questions.size}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (currentIdx + 1).toFloat() / quiz.questions.size },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Question Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = currentQuestion.text,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 22.sp,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Options List
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                currentQuestion.options.forEachIndexed { optIndex, optionText ->
                    val isOptionSelected = selectedOption == optIndex
                    val isCorrectOption = optIndex == currentQuestion.correctIndex

                    val borderColor = when {
                        isSubmitted && isCorrectOption -> Color(0xFF10B981)
                        isSubmitted && isOptionSelected && !isCorrectOption -> Color(0xFFEF4444)
                        isOptionSelected -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    }

                    val containerColor = when {
                        isSubmitted && isCorrectOption -> Color(0xFF10B981).copy(alpha = 0.15f)
                        isSubmitted && isOptionSelected && !isCorrectOption -> Color(0xFFEF4444).copy(alpha = 0.15f)
                        isOptionSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        else -> MaterialTheme.colorScheme.surface
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isSubmitted && !uiState.isReviewMode) {
                                viewModel.selectOption(optIndex)
                            },
                        colors = CardDefaults.cardColors(containerColor = containerColor),
                        border = BorderStroke(1.5.dp, borderColor),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isOptionSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${('A'.code + optIndex).toChar()}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isOptionSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = optionText,
                                style = MaterialTheme.typography.bodyMedium,
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Explanation Section
            if (isSubmitted || uiState.isReviewMode) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Differential Explanation",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = currentQuestion.explanation,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        // Bottom Navigation Buttons
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (currentIdx > 0) {
                OutlinedButton(
                    onClick = { viewModel.previousQuestion() },
                    modifier = Modifier.weight(1f).height(44.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("PREV", fontSize = 11.sp)
                }
            }

            if (!isSubmitted && !uiState.isReviewMode) {
                Button(
                    onClick = { viewModel.submitAnswer() },
                    enabled = selectedOption != null,
                    modifier = Modifier.weight(1f).height(44.dp)
                ) {
                    Text("SUBMIT", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = { viewModel.nextQuestion() },
                    modifier = Modifier.weight(1f).height(44.dp)
                ) {
                    Text(
                        text = if (currentIdx + 1 < quiz.questions.size) "NEXT" else if (uiState.isReviewMode) "BACK TO RESULT" else "FINISH",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun QuizResultScreen(
    score: Int, 
    total: Int, 
    unitName: String,
    onReview: () -> Unit,
    onRetake: () -> Unit,
    onTakeNew: () -> Unit,
    onGoBack: () -> Unit
) {
    val percentage = if (total > 0) (score.toDouble() / total * 100).toInt() else 0
    
    val congratulationMessage = when {
        percentage == 100 -> "UNSTOPPABLE! 🏆 Perfect score."
        percentage >= 80 -> "EXCELLENT! 🌟 High-yield retention."
        percentage >= 60 -> "SOLID PROGRESS! 📈 Ready for review."
        else -> "DIAGNOSTIC COMPLETE! 🎯 Target weak spots."
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = if (percentage >= 70) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFF59E0B).copy(alpha = 0.15f),
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (percentage >= 70) Icons.Default.EmojiEvents else Icons.Default.Psychology,
                    contentDescription = null,
                    tint = if (percentage >= 70) Color(0xFF10B981) else Color(0xFFF59E0B),
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = congratulationMessage, 
            fontSize = 18.sp, 
            fontWeight = FontWeight.Black, 
            textAlign = TextAlign.Center,
            color = if (percentage >= 80) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        Text(unitName, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
        
        Spacer(modifier = Modifier.height(16.dp))
        Text("$percentage%", fontSize = 48.sp, fontWeight = FontWeight.Black, color = if (percentage >= 70) Color(0xFF10B981) else Color(0xFFEF4444))
        Text("Correct: $score of $total Questions", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.outline)
        
        Spacer(modifier = Modifier.height(30.dp))
        
        // Options Grid
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onReview, 
                    modifier = Modifier.weight(1f).height(44.dp), 
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("REVIEW MISTAKES", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onRetake, 
                    modifier = Modifier.weight(1f).height(44.dp), 
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("RETAKE QUIZ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }
            Button(
                onClick = onTakeNew, 
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("START DIFFERENT TOPIC QUIZ", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = onGoBack) {
                Text("Return to Syllabus Selection", fontSize = 12.sp)
            }
        }
    }
}
