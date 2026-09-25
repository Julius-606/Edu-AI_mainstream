package com.example.edu_ai.ui.screens.student

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.edu_ai.EduAIApplication
import androidx.compose.ui.platform.LocalContext
import com.example.edu_ai.ui.components.DynamicBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    userId: String,
    onBack: () -> Unit,
    onUnitAdded: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as EduAIApplication
    val viewModel: LibraryViewModel = viewModel(
        factory = LibraryViewModel.Factory(app.repository)
    )
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadLibrary()
    }

    // Drill-down Picker State
    var currentStage by remember { mutableStateOf("field") } // "field", "course", "unit"
    var selectedField by remember { mutableStateOf<String?>(null) }
    var selectedCourse by remember { mutableStateOf<String?>(null) }

    // Hardcoded structure for picks
    val fields = remember {
        listOf(
            FieldItem("Medical & Health Sciences", Icons.Default.MedicalServices, Color(0xFF00E5FF)),
            FieldItem("Computer Science & IT", Icons.Default.Computer, Color(0xFF6366F1)),
            FieldItem("Engineering & Applied Sciences", Icons.Default.Engineering, Color(0xFFEC4899)),
            FieldItem("Business & Administration", Icons.Default.BusinessCenter, Color(0xFF10B981))
        )
    }

    val coursesMap = remember {
        mapOf(
            "Medical & Health Sciences" to listOf(
                "Bachelor of Medicine & Bachelor of Surgery (MBChB)",
                "Bachelor of Science in Nursing",
                "Bachelor of Pharmacy"
            ),
            "Computer Science & IT" to listOf(
                "BSc. Software Engineering",
                "BSc. Artificial Intelligence & Data Science",
                "BSc. Cybersecurity & IT Systems"
            ),
            "Engineering & Applied Sciences" to listOf(
                "BSc. Electrical & Electronic Engineering",
                "BSc. Civil & Structural Engineering",
                "BSc. Mechanical & Robotics Engineering"
            ),
            "Business & Administration" to listOf(
                "Bachelor of Business Administration (BBA)",
                "BSc. Financial Engineering",
                "BA. Economics & Statistics"
            )
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        DynamicBackground()

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("Syllabus Library", fontWeight = FontWeight.Black, fontSize = 20.sp)
                            if (selectedField != null) {
                                Text(
                                    text = buildString {
                                        append(selectedField)
                                        if (selectedCourse != null) append(" > ").append(selectedCourse?.take(18)).append("...")
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            when (currentStage) {
                                "unit" -> {
                                    selectedCourse = null
                                    currentStage = "course"
                                }
                                "course" -> {
                                    selectedField = null
                                    currentStage = "field"
                                }
                                else -> onBack()
                            }
                        }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                if (uiState.isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primary)
                }

                if (uiState.error != null) {
                    Text(
                        text = uiState.error!!,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp),
                        fontSize = 13.sp
                    )
                }

                if (uiState.successMessage != null) {
                    Text(
                        text = uiState.successMessage!!,
                        color = Color(0xFF059669),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(16.dp),
                        fontSize = 13.sp
                    )
                }

                androidx.compose.animation.Crossfade(
                    targetState = currentStage,
                    modifier = Modifier.fillMaxSize(),
                    label = "LibraryStageCrossfade"
                ) { stage ->
                    when (stage) {
                        "field" -> {
                            // STAGE 1: Big Fields Grid
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Pick an Academic Field",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(fields) { field ->
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(130.dp)
                                                .clickable {
                                                    com.example.edu_ai.utils.TactileFeedback.triggerSubtleClick(context)
                                                    selectedField = field.name
                                                    currentStage = "course"
                                                },
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                                            ),
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.5.dp,
                                                field.themeColor.copy(alpha = 0.4f)
                                            ),
                                            shape = MaterialTheme.shapes.large
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(12.dp),
                                                verticalArrangement = Arrangement.Center,
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Icon(
                                                    imageVector = field.icon,
                                                    contentDescription = null,
                                                    tint = field.themeColor,
                                                    modifier = Modifier.size(36.dp)
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    text = field.name,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        "course" -> {
                            // STAGE 2: Courses List under Field
                            val courses = coursesMap[selectedField] ?: emptyList()
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Pick a Course Path",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(courses) { course ->
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    com.example.edu_ai.utils.TactileFeedback.triggerSubtleClick(context)
                                                    selectedCourse = course
                                                    currentStage = "unit"
                                                },
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                                            ),
                                            shape = MaterialTheme.shapes.medium
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(16.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = course,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Icon(
                                                    imageVector = Icons.Default.Add,
                                                    contentDescription = "Select",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        "unit" -> {
                            // STAGE 3: Filtered Units Offered
                            // Filter units from available units list
                            val courseLower = selectedCourse?.lowercase() ?: ""
                            val filteredUnits = uiState.availableUnits.filter { unit ->
                                val category = unit.category.lowercase()
                                val name = unit.name.lowercase()
                                when {
                                    courseLower.contains("medicine") || courseLower.contains("surgery") || courseLower.contains("mbchb") -> {
                                        category.contains("medical") || category.contains("clinical") || category.contains("surgery") || category.contains("medicine") || category.contains("biochem") || name.contains("surgery") || name.contains("biochem") || name.contains("medicine")
                                    }
                                    courseLower.contains("nursing") -> {
                                        category.contains("nursing") || name.contains("nursing") || name.contains("clinical")
                                    }
                                    courseLower.contains("pharmacy") -> {
                                        category.contains("pharmacy") || name.contains("pharmacology") || name.contains("biochem")
                                    }
                                    courseLower.contains("software") || courseLower.contains("computer") -> {
                                        category.contains("software") || category.contains("computer") || category.contains("it") || name.contains("software") || name.contains("database") || name.contains("programming")
                                    }
                                    courseLower.contains("intelligence") || courseLower.contains("data") -> {
                                        category.contains("ai") || category.contains("data") || category.contains("intelligence") || name.contains("ai") || name.contains("intelligence") || name.contains("python")
                                    }
                                    courseLower.contains("electrical") -> {
                                        category.contains("electrical") || name.contains("circuit") || name.contains("electrical")
                                    }
                                    courseLower.contains("civil") -> {
                                        category.contains("civil") || name.contains("structural") || name.contains("survey")
                                    }
                                    courseLower.contains("business") || courseLower.contains("administration") -> {
                                        category.contains("business") || category.contains("management") || name.contains("accounting") || name.contains("marketing")
                                    }
                                    courseLower.contains("finance") || courseLower.contains("financial") -> {
                                        category.contains("finance") || name.contains("finance") || name.contains("quantitative")
                                    }
                                    else -> {
                                        // General fallback: return true if not matched to anything, or filter generally
                                        true
                                    }
                                }
                            }.ifEmpty {
                                // If course filter ends up empty, show all available units in the library as general electives
                                uiState.availableUnits
                            }

                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Units Offered under $selectedCourse",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )

                                if (filteredUnits.isEmpty()) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text("No specialized units found for this path.", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                                    }
                                } else {
                                    LazyColumn(
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        items(filteredUnits) { unit ->
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                                                ),
                                                shape = MaterialTheme.shapes.large
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(14.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        Icons.Default.LibraryBooks,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            unit.name,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 14.sp
                                                        )
                                                        Text(
                                                            unit.category,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            fontSize = 10.sp,
                                                            color = MaterialTheme.colorScheme.outline
                                                        )
                                                    }
                                                    Button(
                                                        onClick = {
                                                            com.example.edu_ai.utils.TactileFeedback.triggerSubtleClick(context)
                                                            viewModel.addUnit(unit.id, userId, onUnitAdded)
                                                        },
                                                        shape = MaterialTheme.shapes.medium,
                                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                                        modifier = Modifier.height(32.dp)
                                                    ) {
                                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("ADD", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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

data class FieldItem(val name: String, val icon: ImageVector, val themeColor: Color)
