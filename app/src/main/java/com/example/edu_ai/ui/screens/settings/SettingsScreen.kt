package com.example.edu_ai.ui.screens.settings

import android.text.format.DateFormat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.edu_ai.data.remote.ApiAppRelease
import com.example.edu_ai.repository.EduAIRepository
import com.example.edu_ai.utils.PreferenceManager
import com.example.edu_ai.utils.UpdateManager
import kotlinx.coroutines.launch
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    userId: String,
    repository: EduAIRepository,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val isChecking by UpdateManager.isChecking.collectAsState()
    val releases by UpdateManager.releases.collectAsState()
    val latestRelease by UpdateManager.latestRelease.collectAsState()
    val mandatoryRequired by UpdateManager.mandatoryUpdateRequired.collectAsState()
    val lastCheckedTime by UpdateManager.lastCheckedTime.collectAsState()

    var showAllReleases by remember { mutableStateOf(true) }
    var syncMessage by remember { mutableStateOf<String?>(null) }
    var isSyncing by remember { mutableStateOf(false) }

    var selectedBackendMode by remember { mutableStateOf(PreferenceManager.getBackendMode(context)) }
    val activeUnits by repository.dao.getAllUnits().collectAsState(initial = emptyList())
    var isUnitsSectionExpanded by remember { mutableStateOf(true) }
    var expandedUnitDeleteId by remember { mutableStateOf<Long?>(null) }

    // Theme, Font & Accent Customization
    var appTheme by remember { mutableStateOf(PreferenceManager.getAppTheme(context)) }
    var themeAccent by remember { mutableStateOf(PreferenceManager.getThemeAccent(context)) }
    var appFont by remember { mutableStateOf(PreferenceManager.getAppFont(context)) }

    // Account Profile Settings
    var profileName by remember { mutableStateOf(PreferenceManager.getUserDisplayName(context)) }
    var profileEmail by remember { mutableStateOf(PreferenceManager.getUserEmail(context)) }
    var academicLevel by remember { mutableStateOf(PreferenceManager.getAcademicLevel(context)) }
    var aiPersona by remember { mutableStateOf(PreferenceManager.getAiPersona(context)) }
    var difficulty by remember { mutableStateOf(PreferenceManager.getDifficulty(context)) }
    var profileSavedMessage by remember { mutableStateOf<String?>(null) }

    // Auto-fetch fresh releases from the admin's archive upon opening the screen
    LaunchedEffect(Unit) {
        UpdateManager.checkForUpdates(repository)
    }

    val currentCode = UpdateManager.CURRENT_VERSION_CODE
    val latestCode = latestRelease?.versionCode ?: currentCode
    val hasNewerRelease = latestCode > currentCode

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Settings & Updates",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "App Version, Archives & Preferences",
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
                actions = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                UpdateManager.checkForUpdates(repository)
                            }
                        },
                        enabled = !isChecking
                    ) {
                        if (isChecking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh Archive")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // --- SECTION 1: VERSION & SYSTEM RELEASES (PRIMARY TASK) ---
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "App Version & Releases",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (isChecking) {
                        Text(
                            text = "Syncing with archive...",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else if (lastCheckedTime > 0) {
                        Text(
                            text = "Checked: ${DateFormat.format("hh:mm a", Date(lastCheckedTime))}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            // Current Version Card with Status
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            mandatoryRequired -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                            hasNewerRelease -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            mandatoryRequired -> MaterialTheme.colorScheme.error
                                            hasNewerRelease -> MaterialTheme.colorScheme.tertiary
                                            else -> MaterialTheme.colorScheme.primary
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when {
                                        mandatoryRequired -> Icons.Default.SecurityUpdateWarning
                                        hasNewerRelease -> Icons.Default.SystemUpdate
                                        else -> Icons.Default.CheckCircle
                                    },
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Trace Academic Engine",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "Installed: v${UpdateManager.CURRENT_VERSION_NAME} (Build ${UpdateManager.CURRENT_VERSION_CODE})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Status Pill
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = when {
                                    mandatoryRequired -> MaterialTheme.colorScheme.error
                                    hasNewerRelease -> MaterialTheme.colorScheme.tertiary
                                    else -> MaterialTheme.colorScheme.primaryContainer
                                }
                            ) {
                                Text(
                                    text = when {
                                        mandatoryRequired -> "MANDATORY"
                                        hasNewerRelease -> "UPDATE AVAIL"
                                        else -> "UP TO DATE"
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = when {
                                        mandatoryRequired || hasNewerRelease -> Color.White
                                        else -> MaterialTheme.colorScheme.onPrimaryContainer
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Explanatory message
                        Text(
                            text = when {
                                mandatoryRequired -> "🚨 The administrator has triggered a strict mandatory update lock in the system archives. Update immediately to prevent service disruption."
                                hasNewerRelease -> "A new version (${latestRelease?.version ?: "Latest"}) is archived and available for download with new features and fixes."
                                else -> "You are on the latest version. This section automatically listens for new releases archived by the admin."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Action button
                        if (hasNewerRelease || mandatoryRequired) {
                            Button(
                                onClick = {
                                    val url = latestRelease?.downloadUrl ?: "https://github.com/Agent606/Trace/releases/latest"
                                    UpdateManager.openDownloadUrl(context, url)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (mandatoryRequired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Download Latest Build (${latestRelease?.version ?: "v1.1.0"})",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        UpdateManager.checkForUpdates(repository)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                enabled = !isChecking
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (isChecking) "Checking Archives..." else "Check for Updates Now")
                            }
                        }
                    }
                }
            }

            // Archived Releases Header & Toggle
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAllReleases = !showAllReleases },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.HistoryEdu, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Admin Release Archive (${releases.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = { showAllReleases = !showAllReleases }) {
                        Icon(
                            imageVector = if (showAllReleases) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Toggle Releases"
                        )
                    }
                }
            }

            // List of archived releases from backend
            if (showAllReleases) {
                if (releases.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "No release archives logged yet.",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                } else {
                    items(releases, key = { it.id }) { release ->
                        ReleaseItemCard(
                            release = release,
                            currentVersionCode = currentCode,
                            onDownload = { url ->
                                UpdateManager.openDownloadUrl(context, url)
                            }
                        )
                    }
                }
            }

            // --- SECTION 1.5: MANAGE ACTIVE UNITS (COLLAPSIBLE) ---
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isUnitsSectionExpanded = !isUnitsSectionExpanded }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.School,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Manage Active Units (${activeUnits.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Icon(
                        imageVector = if (isUnitsSectionExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Toggle Units Section",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            item {
                AnimatedVisibility(visible = isUnitsSectionExpanded) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "De-clutter your study space by deactivating or deleting syllabus units. Deletion controls are parented in collapsible drawers to prevent accidental loss.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )

                            if (activeUnits.isEmpty()) {
                                Text(
                                    text = "No active units in cache.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            } else {
                                activeUnits.forEach { unit ->
                                    val isDeleteOpen = expandedUnitDeleteId == unit.localId

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = unit.unitName,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = "Local ID: ${unit.localId}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.outline
                                                    )
                                                }

                                                // Collapsible trigger for delete
                                                OutlinedButton(
                                                    onClick = {
                                                        expandedUnitDeleteId = if (isDeleteOpen) null else unit.localId
                                                    },
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                    modifier = Modifier.height(30.dp),
                                                    colors = ButtonDefaults.outlinedButtonColors(
                                                        contentColor = if (isDeleteOpen) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                                                    ),
                                                    border = BorderStroke(
                                                        1.dp,
                                                        if (isDeleteOpen) MaterialTheme.colorScheme.error.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                                    )
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Delete", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                                    Spacer(modifier = Modifier.width(2.dp))
                                                    Icon(
                                                        imageVector = if (isDeleteOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                }
                                            }

                                            // Collapsible Delete Confirmation Drawer
                                            AnimatedVisibility(visible = isDeleteOpen) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(top = 10.dp)
                                                        .background(
                                                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f),
                                                            shape = RoundedCornerShape(8.dp)
                                                        )
                                                        .padding(10.dp)
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                            Icons.Default.Warning,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.error,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(
                                                            "Danger Zone: Delete Unit",
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 11.sp,
                                                            color = MaterialTheme.colorScheme.error
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        "Permanently remove ${unit.unitName} and its cascading modules, topics, and subtopics from local cache and remote sync.",
                                                        fontSize = 10.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.End
                                                    ) {
                                                        TextButton(
                                                            onClick = { expandedUnitDeleteId = null },
                                                            modifier = Modifier.height(30.dp)
                                                        ) {
                                                            Text("Cancel", fontSize = 10.sp)
                                                        }
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Button(
                                                            onClick = {
                                                                scope.launch {
                                                                    repository.dao.deleteUnitById(unit.localId)
                                                                    expandedUnitDeleteId = null
                                                                }
                                                            },
                                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                                            shape = RoundedCornerShape(8.dp),
                                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                                            modifier = Modifier.height(30.dp)
                                                        ) {
                                                            Text("Confirm Delete", fontSize = 10.sp, fontWeight = FontWeight.Bold)
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

            // --- SECTION 2: STUDY SYNCHRONIZATION & STORAGE ---
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Cloud Sync & Local Cache",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Offline Database State", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Encrypted Room SQLite DB", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            }
                            FilledTonalButton(
                                onClick = {
                                    scope.launch {
                                        isSyncing = true
                                        try {
                                            repository.triggerSync(userId)
                                            syncMessage = "Synchronized all progress and bookmarks with cloud server."
                                        } catch (e: Exception) {
                                            syncMessage = "Sync failed: ${e.message}"
                                        } finally {
                                            isSyncing = false
                                        }
                                    }
                                },
                                enabled = !isSyncing
                            ) {
                                if (isSyncing) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Sync Now")
                                }
                            }
                        }

                        if (syncMessage != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = syncMessage!!,
                                    modifier = Modifier.padding(10.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }

            // --- SECTION 3: SYSTEM CONNECTION & GATEWAY ---
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Server Gateway & Diagnostics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Active Backend Target:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("cloud" to "Cloud", "ngrok" to "Ngrok", "container" to "Container").forEach { (key, label) ->
                                val isSelected = selectedBackendMode == key
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedBackendMode = key
                                        PreferenceManager.saveBackendMode(context, key)
                                    },
                                    label = { Text(label) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Trace Engine Build:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            Text("2026.09-Release-A", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("FastAPI Wiretap & Telemetry:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            Text("ONLINE", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            // --- SECTION 4: APPEARANCE, THEME COLORS & FONTS ---
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Appearance, Themes & Fonts",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        // 1. Theme Mode
                        Text(
                            text = "App Theme Mode:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "dark" to "Dark Slate",
                                "light" to "Light",
                                "oled" to "OLED Black",
                                "system" to "System"
                            ).forEach { (modeKey, label) ->
                                val isSelected = appTheme == modeKey
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        appTheme = modeKey
                                        PreferenceManager.saveAppTheme(context, modeKey)
                                    },
                                    label = { Text(label, fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                        // 2. Theme Accent Color
                        Text(
                            text = "Theme Accent Color:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                Triple("indigo", "Indigo", Color(0xFF6366F1)),
                                Triple("emerald", "Emerald", Color(0xFF10B981)),
                                Triple("violet", "Violet", Color(0xFF8B5CF6)),
                                Triple("coral", "Coral", Color(0xFFF43F5E)),
                                Triple("amber", "Amber", Color(0xFFF59E0B))
                            ).forEach { (accentKey, label, colorVal) ->
                                val isSelected = themeAccent == accentKey
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        themeAccent = accentKey
                                        PreferenceManager.saveThemeAccent(context, accentKey)
                                    },
                                    leadingIcon = {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(colorVal)
                                        )
                                    },
                                    label = { Text(label, fontSize = 10.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                        // 3. App Typography & Font Family
                        Text(
                            text = "App Typography / Font Family:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "sans" to "Modern Sans",
                                "serif" to "Editorial Serif",
                                "monospace" to "Technical Mono"
                            ).forEach { (fontKey, label) ->
                                val isSelected = appFont == fontKey
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        appFont = fontKey
                                        PreferenceManager.saveAppFont(context, fontKey)
                                    },
                                    label = { Text(label, fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // --- SECTION 5: ACCOUNT & PROFILE CREDENTIALS ---
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Account Profile & Learning Persona",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = profileName,
                            onValueChange = { profileName = it },
                            label = { Text("Student Full Name", fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = profileEmail,
                            onValueChange = { profileEmail = it },
                            label = { Text("Institutional Email", fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = "Academic Level:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "Year 1 - Pre-Clinical" to "Pre-Clinical",
                                "Year 2 - Foundations" to "Foundations",
                                "Year 3 - Core Clerkships" to "Clerkships",
                                "Year 4 - Rotations" to "Rotations"
                            ).forEach { (levelVal, label) ->
                                val isSelected = academicLevel == levelVal
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { academicLevel = levelVal },
                                    label = { Text(label, fontSize = 10.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Text(
                            text = "AI Consultant Persona:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "Socratic Tutor" to "Socratic",
                                "Clinical Attending" to "Attending",
                                "Exam Drillmaster" to "Drillmaster",
                                "Feynman Explainer" to "Feynman"
                            ).forEach { (personaVal, label) ->
                                val isSelected = aiPersona == personaVal
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { aiPersona = personaVal },
                                    label = { Text(label, fontSize = 10.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        if (profileSavedMessage != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = profileSavedMessage!!,
                                    modifier = Modifier.padding(8.dp),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        Button(
                            onClick = {
                                PreferenceManager.saveUserProfile(
                                    context,
                                    profileName.trim(),
                                    profileEmail.trim(),
                                    academicLevel,
                                    aiPersona,
                                    difficulty
                                )
                                scope.launch {
                                    try {
                                        repository.triggerSync(userId)
                                    } catch (_: Exception) {}
                                }
                                profileSavedMessage = "Account profile and AI tutor persona saved successfully."
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save Account Changes", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReleaseItemCard(
    release: ApiAppRelease,
    currentVersionCode: Int,
    onDownload: (String) -> Unit
) {
    val isCurrentInstalled = release.versionCode == currentVersionCode
    val isNewer = release.versionCode > currentVersionCode

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                release.isMandatory -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                isCurrentInstalled -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "v${release.version}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = if (release.isMandatory) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "Build ${release.versionCode}",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Badges
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (release.isMandatory) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.error
                        ) {
                            Text(
                                text = "MANDATORY",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    }
                    if (isCurrentInstalled) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = "CURRENT",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = release.artifactType,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = release.fileSize,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            if (!release.releaseNotes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = release.releaseNotes,
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = { onDownload(release.downloadUrl) },
                    shape = RoundedCornerShape(10.dp),
                    colors = if (release.isMandatory) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error) else ButtonDefaults.buttonColors(),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isCurrentInstalled) "Re-download" else "Download Version",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
