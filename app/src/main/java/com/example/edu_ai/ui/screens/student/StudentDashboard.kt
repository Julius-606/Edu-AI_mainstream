package com.example.edu_ai.ui.screens.student

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.edu_ai.data.local.ModuleEntity
import com.example.edu_ai.data.local.ModuleWithTopics
import com.example.edu_ai.data.local.SubtopicEntity
import com.example.edu_ai.data.local.TopicEntity
import com.example.edu_ai.data.local.TopicWithSubtopics
import com.example.edu_ai.data.local.UnitEntity
import com.example.edu_ai.data.local.UnitWithModules
import com.example.edu_ai.data.local.UserEntity
import com.example.edu_ai.data.remote.ApiTimetableSlot
import com.example.edu_ai.ui.components.DynamicBackground
import com.example.edu_ai.ui.components.ProgressRings
import com.example.edu_ai.ui.components.RingProgress
import com.example.edu_ai.ui.theme.TraceTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDashboard(
    userId: String,
    onLogout: () -> Unit,
    onLaunchModule: (Int?) -> Unit,
    onOpenLibrary: () -> Unit,
    onViewUnitOutline: (Long) -> Unit,
    onOpenConsultations: () -> Unit,
    viewModel: StudentViewModel = viewModel(factory = StudentViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    
    val user = uiState.user ?: UserEntity(id = userId, username = "Student", role = "student", difficulty = "Medium (Standard)", semesterStatus = "Active", aiPersona = "Helper")

    val progressViewModel: ProgressViewModel = viewModel(factory = ProgressViewModel.provideFactory(user))
    val recommendation by progressViewModel.recommendation.collectAsState()
    
    val timetableViewModel: TimetableViewModel = viewModel(factory = TimetableViewModel.provideFactory(user))
    val timetableUiState by timetableViewModel.uiState.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var showAccountDialog by remember { mutableStateOf(false) }
    var showManageUnitsDialog by remember { mutableStateOf(false) }
    var showArchivesDialog by remember { mutableStateOf(false) }
    var unitToDelete by remember { mutableStateOf<UnitEntity?>(null) }

    LaunchedEffect(userId) {
        viewModel.refreshDashboard(userId)
        progressViewModel.refreshRecommendations()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text("Trace Navigation", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(user.username, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Person, contentDescription = null) },
                        label = { Text("Account") },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            showAccountDialog = true
                        }
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Archive, contentDescription = null) },
                        label = { Text("Archives") },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            showArchivesDialog = true
                        }
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.School, contentDescription = null) },
                        label = { Text("Manage Units") },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            showManageUnitsDialog = true
                        }
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null) },
                        label = { Text("Consultations") },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onOpenConsultations()
                        }
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Quiz, contentDescription = null) },
                        label = { Text("Quizzes") },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onOpenConsultations()
                        }
                    )
                }
            }
        }
    ) {
        StudentDashboardContent(
            user = user,
            uiState = uiState,
            recommendation = recommendation,
            timetableUiState = timetableUiState,
            onOpenDrawer = { scope.launch { drawerState.open() } },
            onLogout = onLogout,
            onLaunchModule = onLaunchModule,
            onOpenLibrary = onOpenLibrary,
            onViewUnitOutline = onViewUnitOutline,
            onOpenConsultations = onOpenConsultations
        )
    }

    // Account Management Dialog
    if (showAccountDialog) {
        var difficulty by remember { mutableStateOf(user.difficulty) }
        var aiPersona by remember { mutableStateOf(user.aiPersona) }
        var semesterStatus by remember { mutableStateOf(user.semesterStatus) }

        AlertDialog(
            onDismissRequest = { showAccountDialog = false },
            title = { Text("Account Management") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("User: ${user.username}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    if (user.email.isNotEmpty()) {
                        Text("Email: ${user.email}", style = MaterialTheme.typography.bodySmall)
                    }
                    
                    OutlinedTextField(
                        value = semesterStatus,
                        onValueChange = { semesterStatus = it },
                        label = { Text("Academic Level / Semester") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = difficulty,
                        onValueChange = { difficulty = it },
                        label = { Text("Target Difficulty") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = aiPersona,
                        onValueChange = { aiPersona = it },
                        label = { Text("AI Persona / Consultant Style") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Button(
                        onClick = {
                            showAccountDialog = false
                            onLogout()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.ExitToApp, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SIGN OUT / LOGOUT")
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateUserProfile(difficulty, aiPersona, semesterStatus)
                    showAccountDialog = false
                }) {
                    Text("SAVE CHANGES")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAccountDialog = false }) {
                    Text("CANCEL")
                }
            }
        )
    }

    // Manage Units Dialog
    if (showManageUnitsDialog) {
        AlertDialog(
            onDismissRequest = { showManageUnitsDialog = false },
            title = { Text("Manage Ongoing Units") },
            text = {
                Column {
                    Text("Active Units:", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (uiState.units.isEmpty()) {
                        Text("No active units. Add units from the library.")
                    } else {
                        uiState.units.forEach { unit ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(unit.unitName, modifier = Modifier.weight(1f))
                                IconButton(onClick = { unitToDelete = unit }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Unit", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    showManageUnitsDialog = false
                    onOpenLibrary()
                }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ADD UNIT")
                }
            },
            dismissButton = {
                TextButton(onClick = { showManageUnitsDialog = false }) {
                    Text("CLOSE")
                }
            }
        )
    }

    // Delete Unit Confirmation Dialog
    unitToDelete?.let { unit ->
        AlertDialog(
            onDismissRequest = { unitToDelete = null },
            title = { Text("Delete Unit '${unit.unitName}'?") },
            text = { Text("This will remove this ongoing unit and its associated progress.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteUnit(unit.localId)
                    unitToDelete = null
                }) {
                    Text("DELETE", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { unitToDelete = null }) {
                    Text("CANCEL")
                }
            }
        )
    }

    // Archives Dialog
    if (showArchivesDialog) {
        AlertDialog(
            onDismissRequest = { showArchivesDialog = false },
            title = { Text("Archived & Completed Units") },
            text = {
                Column {
                    Text("Access completed unit content and quizzes:", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (uiState.archivedUnits.isEmpty()) {
                        Text("No archived units yet.")
                    } else {
                        uiState.archivedUnits.forEach { unit ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(unit.unitName, modifier = Modifier.weight(1f))
                                TextButton(onClick = {
                                    showArchivesDialog = false
                                    onViewUnitOutline(unit.localId)
                                }) {
                                    Text("VIEW")
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showArchivesDialog = false }) {
                    Text("CLOSE")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDashboardContent(
    user: UserEntity,
    uiState: StudentUiState,
    recommendation: String,
    timetableUiState: TimetableUiState,
    onOpenDrawer: () -> Unit = {},
    onLogout: () -> Unit,
    onLaunchModule: (Int?) -> Unit,
    onOpenLibrary: () -> Unit,
    onViewUnitOutline: (Long) -> Unit,
    onOpenConsultations: () -> Unit
) {
    val viewModel: StudentViewModel = viewModel(factory = StudentViewModel.Factory)
    
    LaunchedEffect(uiState.unitsWithModules) {
        uiState.unitsWithModules.forEach { unitWithModules ->
            val allSubtopics = unitWithModules.modules.flatMap { it.topics }.flatMap { it.subtopics }
            if (allSubtopics.isNotEmpty() && allSubtopics.all { it.isCompleted }) {
                viewModel.archiveUnit(unitWithModules.unit.localId, isActive = false)
            }
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        DynamicBackground()

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(Icons.Default.Menu, contentDescription = "Open Sidebar Menu")
                        }
                    },
                    title = { 
                        Column {
                            Text("Trace Portal", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                            Text("Student Dashboard", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    },
                    actions = {
                        IconButton(onClick = onOpenConsultations) {
                            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Consultations", tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                    )
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }

                // Welcome Section
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Hi, ${user.username}!",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-1).sp
                            )
                            Text(
                                text = user.semesterStatus,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                             FilledTonalButton(
                                onClick = onOpenLibrary,
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("UNIT")
                            }
                        }
                    }
                }

                // Zenith Insight (Prominent)
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                        ),
                        shape = MaterialTheme.shapes.extraLarge
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.AutoAwesome, 
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("ZENITH INSIGHT", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                recommendation,
                                style = MaterialTheme.typography.bodyLarge,
                                lineHeight = 22.sp
                            )
                        }
                    }
                }

                item {
                    Text("Current Progress", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                if (uiState.unitsWithModules.isEmpty() && !uiState.isLoading) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "Your library is empty.", 
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(onClick = onOpenLibrary) {
                                    Text("ADD YOUR FIRST UNIT")
                                }
                            }
                        }
                    }
                }

                // Hierarchical Progress Cards
                items(uiState.unitsWithModules.filter { unitWithModules ->
                    val allSubtopics = unitWithModules.modules.flatMap { it.topics }.flatMap { it.subtopics }
                    allSubtopics.isNotEmpty() && allSubtopics.any { !it.isCompleted }
                }) { unitWithModules ->
                    val unit = unitWithModules.unit
                    
                    val allTopics = unitWithModules.modules.flatMap { it.topics }
                    val allSubtopics = allTopics.flatMap { it.subtopics }
                    
                    val unitProgress = if (allSubtopics.isNotEmpty()) {
                        (allSubtopics.count { it.isCompleted }.toFloat() / allSubtopics.size) * 100f
                    } else 0f

                    var activeModule: ModuleEntity? = null
                    var activeTopic: TopicEntity? = null
                    var activeSubtopic: SubtopicEntity? = null

                    outer@ for (mod in unitWithModules.modules) {
                        for (top in mod.topics) {
                            for (sub in top.subtopics) {
                                if (!sub.isCompleted) {
                                    activeModule = mod.module
                                    activeTopic = top.topic
                                    activeSubtopic = sub
                                    break@outer
                                }
                            }
                        }
                    }

                    if (activeSubtopic == null && allSubtopics.isNotEmpty()) {
                        val lastMod = unitWithModules.modules.lastOrNull()
                        val lastTop = lastMod?.topics?.lastOrNull()
                        val lastSub = lastTop?.subtopics?.lastOrNull()
                        activeModule = lastMod?.module
                        activeTopic = lastTop?.topic
                        activeSubtopic = lastSub
                    }

                    val activeModuleWithTopics = unitWithModules.modules.find { it.module.moduleId == activeModule?.moduleId } ?: unitWithModules.modules.firstOrNull()
                    val activeModuleSubtopics = activeModuleWithTopics?.topics?.flatMap { it.subtopics } ?: emptyList()
                    
                    val moduleProgress = if (activeModuleSubtopics.isNotEmpty()) {
                        (activeModuleSubtopics.count { it.isCompleted }.toFloat() / activeModuleSubtopics.size) * 100f
                    } else 0f

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val rings = listOf(
                                RingProgress(unitProgress, MaterialTheme.colorScheme.primary, "Unit"),
                                RingProgress(moduleProgress, MaterialTheme.colorScheme.secondary, "Module"),
                                RingProgress(if (activeSubtopic?.isCompleted == true) 100f else 0f, MaterialTheme.colorScheme.tertiary, "Subtopic")
                            )
                            
                            ProgressRings(
                                rings = rings,
                                learningObjectives = emptyList(),
                                modifier = Modifier.size(120.dp)
                            )
                            
                            Spacer(modifier = Modifier.width(20.dp))
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    unit.unitName, 
                                    fontWeight = FontWeight.ExtraBold, 
                                    fontSize = 20.sp,
                                    lineHeight = 24.sp,
                                    modifier = Modifier.clickable { onViewUnitOutline(unit.localId) }
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                if (activeSubtopic != null) {
                                    if (activeModule != null) {
                                        Text(
                                            "Module: ${activeModule.name}", 
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.secondary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    if (activeTopic != null) {
                                        Text(
                                            "Topic: ${activeTopic.name}", 
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        "Subtopic: ${activeSubtopic.name}", 
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else {
                                    Text(
                                        "Unit Completed", 
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { 
                                        onLaunchModule(activeSubtopic?.subtopicId?.toInt())
                                    },
                                    shape = MaterialTheme.shapes.medium,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("CONTINUE")
                                }
                            }
                        }
                    }
                }

                // Timetable Preview
                item {
                    Text("Today's Schedule", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                val today = java.text.SimpleDateFormat("EEEE", java.util.Locale.getDefault()).format(java.util.Date())
                val todaysSlots = timetableUiState.weeklyPlan.filter { it.day.equals(today, ignoreCase = true) }

                if (todaysSlots.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text(
                                "No sessions scheduled for today. Rest up!", 
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    items(todaysSlots) { slot ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp), 
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(slot.time, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Text(slot.activity, style = MaterialTheme.typography.bodyLarge)
                                }
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text(slot.type) }
                                )
                            }
                        }
                    }
                }

                // Full Weekly Timetable
                item {
                    Text("Weekly Schedule", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                if (timetableUiState.weeklyPlan.isNotEmpty()) {
                    val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
                    items(days) { day ->
                        val slotsForDay = timetableUiState.weeklyPlan.filter { it.day.equals(day, ignoreCase = true) }
                        if (slotsForDay.isNotEmpty()) {
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Text(day, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                slotsForDay.forEach { slot ->
                                    Surface(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                        shape = MaterialTheme.shapes.small
                                    ) {
                                        Row(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("${slot.time}: ${slot.activity}", style = MaterialTheme.typography.bodySmall)
                                            Text(slot.type, style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                item { Spacer(modifier = Modifier.height(40.dp)) }
            }
        }
    }
}
