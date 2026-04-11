package com.example.edu_ai.ui.screens.student

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.edu_ai.data.local.UnitEntity

@Composable
fun QuizTabWrapper(
    user: com.example.edu_ai.data.local.UserEntity?,
    units: List<UnitEntity>,
    viewModel: QuizViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.quiz == null) {
        UnitSelectionScreen(units = units, onUnitSelected = { viewModel.startQuiz(it) }, isLoading = uiState.isLoading, error = uiState.error)
    } else if (uiState.isQuizFinished) {
        QuizResultScreen(
            score = uiState.score,
            total = uiState.quiz?.questions?.size ?: 0,
            onRestart = { viewModel.startQuiz(uiState.quiz?.title ?: "") }
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
    units: List<UnitEntity>,
    onUnitSelected: (String) -> Unit,
    isLoading: Boolean,
    error: String?
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Knowledge Retrieval", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Select a unit from your syllabus to start a personalized quiz.", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        
        Spacer(modifier = Modifier.height(24.dp))

        if (isLoading) {
            CircularProgressIndicator()
            Text("AI is drafting your assessment...", modifier = Modifier.padding(top = 16.dp))
        } else {
            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 16.dp))
            }
            
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(units) { _, unit ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onUnitSelected(unit.unitName) }
                    ) {
                        Text(unit.unitName, modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Medium)
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
    val currentQuestion = quiz.questions[currentIdx]
    val selectedIdx = uiState.selectedOptions[currentIdx]
    val isSubmitted = uiState.submittedQuestions.contains(currentIdx)

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Progress
        LinearProgressIndicator(
            progress = { (currentIdx + 1).toFloat() / quiz.questions.size },
            modifier = Modifier.fillMaxWidth()
        )
        Text("Question ${currentIdx + 1} of ${quiz.questions.size}", style = MaterialTheme.typography.labelMedium)

        // Question
        Text(text = currentQuestion.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)

        // Options
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

        // Explanation
        if (isSubmitted) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Explanation", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(currentQuestion.explanation, fontSize = 14.sp)
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Navigation Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Previous Button
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
                // Submit Button
                Button(
                    onClick = { viewModel.submitAnswer() },
                    enabled = selectedIdx != null,
                    modifier = Modifier.weight(1f).height(56.dp)
                ) {
                    Text("SUBMIT")
                }
            } else {
                // Next Button
                Button(
                    onClick = { viewModel.nextQuestion() },
                    modifier = Modifier.weight(1f).height(56.dp)
                ) {
                    Text(if (currentIdx + 1 < quiz.questions.size) "NEXT" else "FINISH")
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                }
            }
        }
    }
}

@Composable
fun QuizResultScreen(score: Int, total: Int, onRestart: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Assessment Complete!", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Your PnL (Accuracy)", fontSize = 18.sp)
        Text("${(score.toDouble()/total * 100).toInt()}%", fontSize = 48.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
        Text("Score: $score / $total", fontSize = 20.sp)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(onClick = onRestart, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text("RETAKE ASSESSMENT")
        }
    }
}
