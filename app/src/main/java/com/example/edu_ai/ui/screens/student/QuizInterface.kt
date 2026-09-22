
package com.example.edu_ai.ui.screens.student

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
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
            onReview = { viewModel.enterReviewMode() },
            onRetake = { viewModel.startQuiz(uiState.quiz?.title ?: "") },
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
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Knowledge Retrieval", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Select a unit from your syllabus to start a personalized quiz.", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        
        Spacer(modifier = Modifier.height(24.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("AI is drafting your assessment...", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 16.dp))
            }
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.School, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Active Contracts (Units)", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                }

                items(unitsWithModules) { unitWithModules ->
                    val unit = unitWithModules.unit
                    val isSelected = selectedUnit == unit.unitName
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onUnitClicked(unit.unitName) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(unit.unitName, fontWeight = FontWeight.Bold)
                            
                            AnimatedVisibility(visible = isSelected) {
                                Column {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    unitWithModules.modules.forEach { moduleWithTopics ->
                                        ModuleAccordion(
                                            moduleWithTopics = moduleWithTopics,
                                            onSubtopicClicked = { onStartQuiz(unit.unitName, it) }
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(12.dp))
                                    OutlinedTextField(
                                        value = focusArea,
                                        onValueChange = { focusArea = it },
                                        placeholder = { Text("Area of focus (Optional)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                                        shape = MaterialTheme.shapes.medium
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = { onStartQuiz(unit.unitName, focusArea.ifBlank { null }) },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("GENERATE FULL UNIT QUIZ 🧠")
                                    }
                                }
                            }
                        }
                    }
                }

                if (history.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Recent Trades (Performance)", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                    }

                    items(history) { record ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(record.unitName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(
                                        SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(record.timestamp)),
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                
                                val pnlColor = if (record.pnlScore >= 70) Color(0xFF4CAF50) else Color(0xFFF44336)
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        "${record.pnlScore.toInt()}%",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp,
                                        color = pnlColor
                                    )
                                    Text("PnL", fontSize = 10.sp, color = pnlColor)
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
    moduleWithTopics: ModuleWithTopics,
    onSubtopicClicked: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
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
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                moduleWithTopics.module.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        if (expanded) {
            moduleWithTopics.topics.flatMap { it.subtopics }.forEach { subtopic ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSubtopicClicked(subtopic.name) }
                        .padding(start = 28.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (subtopic.isCompleted) Icons.Default.CheckCircle else Icons.Default.School,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (subtopic.isCompleted) Color(0xFF4CAF50) else MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        subtopic.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
    val currentQuestion = quiz.questions[currentIdx]
    val selectedIdx = uiState.selectedOptions[currentIdx]
    val isSubmitted = uiState.submittedQuestions.contains(currentIdx) || uiState.isReviewMode
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LinearProgressIndicator(
                progress = { (currentIdx + 1).toFloat() / quiz.questions.size },
                modifier = Modifier.fillMaxWidth()
            )
            Text("Question ${currentIdx + 1} of ${quiz.questions.size}", style = MaterialTheme.typography.labelMedium)

            FormattedText(text = currentQuestion.text, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))

            currentQuestion.options.forEachIndexed { index, option ->
                val isThisOptionSelected = selectedIdx == index
                val isCorrect = index == currentQuestion.correctIndex
                
                val borderColor = when {
                    isSubmitted && isCorrect -> Color.Green
                    isSubmitted && isThisOptionSelected && !isCorrect -> Color.Red
                    isThisOptionSelected -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.outline
                }

                OutlinedCard(
                    onClick = { if (!isSubmitted) viewModel.selectOption(index) },
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(2.dp, borderColor)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = option, modifier = Modifier.weight(1f))
                        if (isSubmitted) {
                            if (isCorrect) Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.Green)
                            else if (isThisOptionSelected) Icon(Icons.Default.Error, contentDescription = null, tint = Color.Red)
                        }
                    }
                }
            }

            if (isSubmitted) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Explanation", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        FormattedText(text = currentQuestion.explanation, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.previousQuestion() },
                enabled = currentIdx > 0,
                modifier = Modifier.weight(1f).height(56.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("PREV")
            }

            if (!isSubmitted) {
                Button(
                    onClick = { viewModel.submitAnswer() },
                    enabled = selectedIdx != null,
                    modifier = Modifier.weight(1f).height(56.dp)
                ) {
                    Text("SUBMIT")
                }
            } else {
                Button(
                    onClick = { viewModel.nextQuestion() },
                    modifier = Modifier.weight(1f).height(56.dp)
                ) {
                    Text(if (currentIdx + 1 < quiz.questions.size) "NEXT" else if (uiState.isReviewMode) "BACK TO RESULT" else "FINISH")
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                }
            }
        }
    }
}

@Composable
fun QuizResultScreen(
    score: Int, 
    total: Int, 
    onReview: () -> Unit,
    onRetake: () -> Unit,
    onTakeNew: () -> Unit,
    onGoBack: () -> Unit
) {
    val percentage = (score.toDouble() / total * 100).toInt()
    
    val congratulationMessage = when {
        percentage == 100 -> "UNSTOPPABLE! 🏆 Perfect score. You've mastered this contract."
        percentage >= 80 -> "EXCELLENT! 🌟 High-tier performance. You're dominating the syllabus."
        else -> "Assessment Complete!"
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = congratulationMessage, 
            fontSize = 24.sp, 
            fontWeight = FontWeight.Bold, 
            textAlign = TextAlign.Center,
            color = if (percentage >= 80) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        Text("Your PnL (Accuracy)", fontSize = 16.sp)
        Text("$percentage%", fontSize = 64.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
        Text("Score: $score / $total", fontSize = 20.sp)
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // Options Grid
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onReview, modifier = Modifier.weight(1f).height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) {
                    Icon(Icons.Default.Search, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("REVIEW")
                }
                Button(onClick = onRetake, modifier = Modifier.weight(1f).height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("RETAKE")
                }
            }
            Button(onClick = onTakeNew, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Text("TAKE NEW ASSESSMENT")
            }
            TextButton(onClick = onGoBack) {
                Text("Go Back to Units")
            }
        }
    }
}


 