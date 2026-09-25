package com.example.edu_ai.ui.screens.student

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.edu_ai.data.local.BookmarkEntity
import com.example.edu_ai.data.local.SubtopicEntity
import com.example.edu_ai.data.remote.ChatRequest
import com.example.edu_ai.ui.components.DynamicBackground
import com.example.edu_ai.ui.components.FormattedText
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearnScreen(
    userId: String,
    subtopicId: Long,
    onBack: () -> Unit,
    onNavigateToBrowser: (String) -> Unit,
    studentViewModel: StudentViewModel = viewModel(factory = StudentViewModel.Factory)
) {
    val uiState by studentViewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    // Find subtopic from memory state
    var targetSubtopic: SubtopicEntity? = null
    var targetUnitName = "Clinical Science"
    
    for (unitWithMod in uiState.unitsWithModules) {
        for (modWithTop in unitWithMod.modules) {
            for (topWithSub in modWithTop.topics) {
                val found = topWithSub.subtopics.find { it.subtopicId == subtopicId }
                if (found != null) {
                    targetSubtopic = found
                    targetUnitName = unitWithMod.unit.unitName
                    break
                }
            }
        }
    }

    val subtopic = targetSubtopic ?: SubtopicEntity(
        subtopicId = subtopicId,
        topicId = 0L,
        name = "Regulation of PFK-1 in Erythrocytes",
        isCompleted = false
    )

    // Educational Slide Objectives - Formulated dynamically based on the subtopic
    val objectives = remember(subtopic.name) {
        listOf(
            SyllabusObjective(
                title = "Pathophysiology & Molecular Mechanics",
                prompt = "Teach me the core cellular pathophysiology, biochemical regulation, and molecular mechanics of '${subtopic.name}'. Structure the notes with high-yield bullet points suitable for medical board exams."
            ),
            SyllabusObjective(
                title = "Clinical Presentation & Diagnoses",
                prompt = "Teach me the clinical presentations, typical patient symptoms, laboratory diagnostic markers, and diagnostic confirmation protocols for '${subtopic.name}'."
            ),
            SyllabusObjective(
                title = "Exam Traps & Therapeutic Guidelines",
                prompt = "Explain high-yield medical board exam traps, distractors, therapeutic guidelines, and advanced patient management parameters regarding '${subtopic.name}'."
            )
        )
    }

    var isStudyingStarted by remember { mutableStateOf(false) }
    var currentStep by remember { mutableIntStateOf(0) }
    var bookmarkNote by remember { mutableStateOf("") }
    var showBookmarkDialog by remember { mutableStateOf(false) }
    var isBookmarked by remember { mutableStateOf(false) }

    // Dynamic study notes cache
    val studyNotes = remember { mutableStateMapOf<Int, String>() }
    val studyLoading = remember { mutableStateMapOf<Int, Boolean>() }
    val studyErrors = remember { mutableStateMapOf<Int, String?>() }

    // Chat AI state
    var aiQuestion by remember { mutableStateOf("") }
    var aiResponse by remember { mutableStateOf<String?>(null) }
    var isAiLoading by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current

    // Check existing bookmarks
    val localBookmarks by studentViewModel.bookmarks.collectAsState(initial = emptyList())
    LaunchedEffect(localBookmarks, subtopicId) {
        isBookmarked = localBookmarks.any { it.target == subtopicId.toString() && it.type == "learn" }
    }

    // Auto-fetch study content from AI when stepping onto an objective
    LaunchedEffect(isStudyingStarted, currentStep) {
        if (isStudyingStarted && studyNotes[currentStep] == null && studyLoading[currentStep] != true) {
            studyLoading[currentStep] = true
            studyErrors[currentStep] = null
            scope.launch {
                try {
                    val prompt = objectives[currentStep].prompt
                    val notes = studentViewModel.repositoryChat(userId, prompt)
                    studyNotes[currentStep] = notes
                } catch (e: Exception) {
                    studyErrors[currentStep] = "Connection timed out. Tap retry to reload Socratic medical insights."
                } finally {
                    studyLoading[currentStep] = false
                }
            }
        }
    }

    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize()) {
        DynamicBackground()

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(subtopic.name, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, maxLines = 1)
                            Text(targetUnitName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            com.example.edu_ai.utils.TactileFeedback.triggerSubtleClick(context)
                            onBack()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        if (isStudyingStarted) {
                            IconButton(onClick = {
                                com.example.edu_ai.utils.TactileFeedback.triggerSubtleClick(context)
                                if (isBookmarked) {
                                    scope.launch {
                                        val bm = localBookmarks.find { it.target == subtopicId.toString() && it.type == "learn" }
                                        if (bm != null) {
                                            studentViewModel.deleteBookmark(bm.id)
                                        }
                                    }
                                } else {
                                    showBookmarkDialog = true
                                }
                            }) {
                                Icon(
                                    imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    contentDescription = "Bookmark",
                                    tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                    )
                )
            }
        ) { padding ->
            androidx.compose.animation.Crossfade(
                targetState = isStudyingStarted,
                modifier = Modifier.padding(padding),
                label = "StudyScreenFade"
            ) { activeStudy ->
                if (!activeStudy) {
                    // STEP 1: Academic Objectives Overview Screen
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.extraLarge,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.MenuBook,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Academic Objectives Overview",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Before you begin, review the academic scope mapped by Zenith AI for this topic.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                        ) {
                            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                objectives.forEachIndexed { i, obj ->
                                    Row(verticalAlignment = Alignment.Top) {
                                        Surface(
                                            shape = androidx.compose.foundation.shape.CircleShape,
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text("${i + 1}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(obj.title, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                            Text("AI will draft specialized deep study notes.", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(36.dp))

                        Button(
                            onClick = {
                                com.example.edu_ai.utils.TactileFeedback.triggerSubtleClick(context)
                                isStudyingStarted = true
                            },
                            shape = MaterialTheme.shapes.large,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("PROCEED TO ACTIVE STUDY 🚀", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // STEP 2: Interactive sliding interface prompting AI on demand
                    val isCurrentLoading = studyLoading[currentStep] == true
                    val currentContent = studyNotes[currentStep]
                    val currentError = studyErrors[currentStep]

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        // Topic objective slide
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .verticalScroll(scrollState),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                            ),
                            shape = MaterialTheme.shapes.large
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text(
                                    text = objectives[currentStep].title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                if (isCurrentLoading) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Text("Socratic AI is formulating core notes...", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                                        }
                                    }
                                } else if (currentError != null) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(currentError, color = MaterialTheme.colorScheme.error, fontSize = 13.sp, textAlign = TextAlign.Center)
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Button(onClick = {
                                            studyLoading[currentStep] = true
                                            studyErrors[currentStep] = null
                                            scope.launch {
                                                try {
                                                    val res = studentViewModel.repositoryChat(userId, objectives[currentStep].prompt)
                                                    studyNotes[currentStep] = res
                                                } catch (e: Exception) {
                                                    studyErrors[currentStep] = "Failed to reconnect to Socratic Engine."
                                                } finally {
                                                    studyLoading[currentStep] = false
                                                }
                                            }
                                        }) {
                                            Text("Retry Connection")
                                        }
                                    }
                                } else {
                                    FormattedText(
                                        text = currentContent ?: "",
                                        onLinkClicked = { link ->
                                            onNavigateToBrowser(link)
                                        }
                                    )
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                // AI Consultation window
                                if (aiResponse != null || isAiLoading) {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 10.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                                        )
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Default.Star,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    "Socratic Consultation",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            if (isAiLoading) {
                                                CircularProgressIndicator(modifier = Modifier.size(20.dp))
                                            } else {
                                                FormattedText(text = aiResponse ?: "")
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Bottom Socratic Inquiry box
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 6.dp)
                                ) {
                                    StudyChip("Explain with analogy") {
                                        com.example.edu_ai.utils.TactileFeedback.triggerSubtleClick(context)
                                        val q = "Please explain the clinical mechanics of '${subtopic.name}' using a simple, relatable medical analogy."
                                        aiQuestion = q
                                        triggerAiConsultation(userId, q, studentViewModel, scope) { loading, res ->
                                            isAiLoading = loading
                                            aiResponse = res
                                        }
                                    }
                                    StudyChip("Common exam traps") {
                                        com.example.edu_ai.utils.TactileFeedback.triggerSubtleClick(context)
                                        val q = "What are the common medical board exam traps, distractors, and high-yield test points regarding '${subtopic.name}'?"
                                        aiQuestion = q
                                        triggerAiConsultation(userId, q, studentViewModel, scope) { loading, res ->
                                            isAiLoading = loading
                                            aiResponse = res
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = aiQuestion,
                                        onValueChange = { aiQuestion = it },
                                        placeholder = { Text("Ask Socratic AI about ${subtopic.name}...", fontSize = 11.sp) },
                                        modifier = Modifier.weight(1f),
                                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                                        maxLines = 2,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    IconButton(
                                        onClick = {
                                            com.example.edu_ai.utils.TactileFeedback.triggerSubtleClick(context)
                                            val q = aiQuestion.trim()
                                            if (q.isNotEmpty()) {
                                                triggerAiConsultation(userId, q, studentViewModel, scope) { loading, res ->
                                                    isAiLoading = loading
                                                    aiResponse = res
                                                }
                                            }
                                        },
                                        enabled = !isAiLoading
                                    ) {
                                        Icon(
                                            Icons.Default.Send,
                                            contentDescription = "Send",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Previous/Next controllers
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    com.example.edu_ai.utils.TactileFeedback.triggerSubtleClick(context)
                                    if (currentStep > 0) currentStep--
                                },
                                enabled = currentStep > 0,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Text("PREVIOUS", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            if (currentStep == objectives.lastIndex) {
                                Button(
                                    onClick = {
                                        com.example.edu_ai.utils.TactileFeedback.triggerSubtleClick(context)
                                        scope.launch {
                                            studentViewModel.toggleSubtopicCompleted(userId, subtopicId, true)
                                        }
                                        onBack()
                                    },
                                    enabled = !isCurrentLoading,
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("COMPLETE & SYNC", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            } else {
                                Button(
                                    onClick = {
                                        com.example.edu_ai.utils.TactileFeedback.triggerSubtleClick(context)
                                        if (currentStep < objectives.lastIndex) currentStep++
                                    },
                                    enabled = !isCurrentLoading,
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Text("NEXT PART", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Bookmark Note Input Dialog
    if (showBookmarkDialog) {
        AlertDialog(
            onDismissRequest = { showBookmarkDialog = false },
            title = { Text("Save Bookmark Clip") },
            text = {
                Column {
                    Text("Add custom clinical notes or quick reminders to this clip.", fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = bookmarkNote,
                        onValueChange = { bookmarkNote = it },
                        placeholder = { Text("E.g., high-yield topic for cardiac pathology exam...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        studentViewModel.addBookmark(
                            userId = userId,
                            type = "learn",
                            title = subtopic.name,
                            target = subtopicId.toString(),
                            context = (studyNotes[currentStep] ?: "").take(160) + "...",
                            notes = bookmarkNote.trim().ifEmpty { null }
                        )
                        isBookmarked = true
                        showBookmarkDialog = false
                        bookmarkNote = ""
                    }
                }) {
                    Text("Save Clip")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBookmarkDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

data class SyllabusObjective(val title: String, val prompt: String)

@Composable
fun StudyChip(text: String, onClick: () -> Unit) {
    SuggestionChip(
        onClick = onClick,
        label = { Text(text, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
    )
}

private fun triggerAiConsultation(
    userId: String,
    aiQuestion: String,
    viewModel: StudentViewModel,
    scope: kotlinx.coroutines.CoroutineScope,
    onResult: (Boolean, String?) -> Unit
) {
    onResult(true, null)
    scope.launch {
        try {
            val response = viewModel.repositoryChat(userId, aiQuestion)
            onResult(false, response)
        } catch (e: Exception) {
            onResult(false, "System was unable to contact modular clinical AI. Please verify your connection status.")
        }
    }
}
