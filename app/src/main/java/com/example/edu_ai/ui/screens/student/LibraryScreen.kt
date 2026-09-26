package com.example.edu_ai.ui.screens.student

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.edu_ai.EduAIApplication
import com.example.edu_ai.data.remote.LibraryUnit
import com.example.edu_ai.ui.components.DynamicBackground
import com.example.edu_ai.utils.TactileFeedback

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

    // Curated fallback matching the 21 authentic units in the backend
    val fallbackUnits = remember {
        listOf(
            LibraryUnit(8, "General Pharmacology", "Medicine"),
            LibraryUnit(30, "INTRODUCTION TO PHILOSOPHY - EEN 114", "Global"),
            LibraryUnit(18, "GENERAL SURGERY I - BCM 314 - Principles, Emergency, GI Tract and Hernias", "Medicine"),
            LibraryUnit(21, "GENERAL SURGERY II - BCM 322 - Hepatobiliary, Urology, Breast, Vascular and Specialty Surgery", "Medicine"),
            LibraryUnit(22, "Obstetrics and Gynaecology I - BCM 317", "Medicine"),
            LibraryUnit(23, "Obstetrics and Gynaecology II - BCM 323 - Pathology and Management", "Medicine"),
            LibraryUnit(31, "ENTREPRENEURSHIP - HSN 425", "Global"),
            LibraryUnit(3, "Internal Medicine I: Cardiopulmonary and Haematology", "Medicine"),
            LibraryUnit(1, "Internal Medicine I (Crash Course): Cardiopulmonary and Haematology", "Medicine"),
            LibraryUnit(5, "Internal Medicine II: Neurology, Nephrology and Endocrinology", "Medicine"),
            LibraryUnit(4, "Internal Medicine II (Crash Course): Neurology, Nephrology and Endocrinology", "Medicine"),
            LibraryUnit(7, "Internal Medicine III: Gastroenterology, Infectious Diseases, Rheumatology and Oncology", "Medicine"),
            LibraryUnit(6, "Internal Medicine III (Crash Course): Gastroenterology, Infectious Diseases, Rheumatology and Oncology", "Medicine"),
            LibraryUnit(32, "BASIC COMPUTER SKILLS - BCM 111", "Global"),
            LibraryUnit(9, "Clinical Pharmacology I: Autonomic, Cardiovascular, Respiratory, Gastrointestinal and Hematology", "Medicine"),
            LibraryUnit(10, "Clinical Pharmacology II: Antimicrobials, CNS, Endocrine, Chemotherapy and Immunomodulators", "Medicine"),
            LibraryUnit(29, "Clinical Pharmacology III - BCM 331 - Comprehensive and Applied Clinical Pharmacology", "Medicine"),
            LibraryUnit(35, "Emergency Medicine and Life Support [ATLS & ACLS]", "Medicine"),
            LibraryUnit(36, "Child Health - BCM 312", "Medicine"),
            LibraryUnit(38, "RESEARCH METHODOLOGY", "Global"),
            LibraryUnit(37, "Research Methodology - HRS 312", "Medicine")
        )
    }

    val displayUnits = if (uiState.availableUnits.isNotEmpty()) uiState.availableUnits else fallbackUnits

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("All") } // "All", "Medicine", "Global", "Surgery", "Pharmacology", "Internal Medicine"
    var addingUnitId by remember { mutableStateOf<Int?>(null) }

    val filteredUnits = remember(displayUnits, searchQuery, selectedCategoryFilter) {
        displayUnits.filter { unit ->
            val matchesSearch = searchQuery.isBlank() ||
                    unit.name.contains(searchQuery, ignoreCase = true) ||
                    unit.category.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedCategoryFilter) {
                "All" -> true
                "Medicine" -> unit.category.equals("Medicine", ignoreCase = true)
                "Global" -> unit.category.equals("Global", ignoreCase = true)
                "Surgery" -> unit.name.contains("surgery", ignoreCase = true)
                "Pharmacology" -> unit.name.contains("pharmacology", ignoreCase = true)
                "Internal Medicine" -> unit.name.contains("internal medicine", ignoreCase = true)
                "Crash Courses" -> unit.name.contains("crash", ignoreCase = true)
                else -> true
            }

            matchesSearch && matchesFilter
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        DynamicBackground()

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("Add Unit to Syllabus", fontWeight = FontWeight.Black, fontSize = 20.sp)
                            Text(
                                text = "Authentic curriculum courses offered on Trace Backend",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                if (uiState.isLoading && addingUnitId == null) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primary)
                }

                if (uiState.error != null) {
                    Text(
                        text = uiState.error!!,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        fontSize = 12.sp
                    )
                }

                if (uiState.successMessage != null) {
                    Surface(
                        color = Color(0xFF10B981).copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = uiState.successMessage!!,
                                color = Color(0xFF10B981),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Search 21+ backend units (e.g. Surgery, Pharmacology)...", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.primary) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                    )
                )

                // Category Filter Chips
                val filterChips = listOf("All", "Medicine", "Global", "Internal Medicine", "Surgery", "Pharmacology", "Crash Courses")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    items(filterChips) { chip ->
                        val isSelected = selectedCategoryFilter == chip
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                TactileFeedback.triggerSubtleClick(context)
                                selectedCategoryFilter = chip
                            },
                            label = { Text(chip, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }

                // Summary Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${filteredUnits.size} Units Available",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Synchronized with Trace Server",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                if (filteredUnits.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No matching units found.", color = MaterialTheme.colorScheme.outline, fontSize = 13.sp)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredUnits) { unit ->
                            UnitBackendCard(
                                unit = unit,
                                isAdding = addingUnitId == unit.id,
                                onAdd = {
                                    TactileFeedback.triggerSubtleClick(context)
                                    addingUnitId = unit.id
                                    viewModel.addUnit(unit.id, userId) {
                                        addingUnitId = null
                                        onUnitAdded()
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

@Composable
fun UnitBackendCard(
    unit: LibraryUnit,
    isAdding: Boolean,
    onAdd: () -> Unit
) {
    val isMedicine = unit.category.equals("Medicine", ignoreCase = true)
    val categoryColor = if (isMedicine) Color(0xFF00E5FF) else Color(0xFF10B981)
    val categoryIcon: ImageVector = if (isMedicine) Icons.Default.MedicalServices else Icons.Default.Public

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, categoryColor.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = categoryColor.copy(alpha = 0.15f),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = categoryIcon,
                        contentDescription = null,
                        tint = categoryColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = categoryColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = unit.category.uppercase(),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            color = categoryColor,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                    if (unit.name.contains("Crash Course", ignoreCase = true)) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFF59E0B).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "CRASH COURSE",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFF59E0B),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = unit.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Button(
                onClick = onAdd,
                enabled = !isAdding,
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier.height(36.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = categoryColor.copy(alpha = 0.85f),
                    contentColor = Color.Black
                )
            ) {
                if (isAdding) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = Color.Black
                    )
                } else {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ADD", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}
