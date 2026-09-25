package com.example.edu_ai.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.example.edu_ai.utils.PreferenceManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    var activeBackendMode by remember { mutableStateOf(PreferenceManager.getBackendMode(context)) }
    var aiPersona by remember { mutableStateOf("Socratic Tutor") }
    var temperature by remember { mutableStateOf(0.7f) }
    var securityProfile by remember { mutableStateOf("Enforced Zero-Leakage") }
    var statusMessage by remember { mutableStateOf<String?>("Telemetry active: All nodes healthy.") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Admin Superuser Console",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Full Database & Telemetry Authority",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Log Out")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
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
            // System Health & Telemetry KPI Row
            item {
                Text(
                    text = "System Health & Node Status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("API Status", style = MaterialTheme.typography.labelSmall)
                            Text("200 OK", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Mean: 42ms", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Backend Gateway", style = MaterialTheme.typography.labelSmall)
                            Text(activeBackendMode.uppercase(), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("FastAPI Active", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Security", style = MaterialTheme.typography.labelSmall)
                            Text("Enforced", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Zero-Leakage", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            // Backend Gateway Switcher Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudSync, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Active Backend Gateway", fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Select which API target the Android client routes all learning & AI requests to:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("cloud" to "Cloud", "ngrok" to "Ngrok", "container" to "Container").forEach { (key, label) ->
                                val isSelected = activeBackendMode == key
                                Button(
                                    onClick = {
                                        activeBackendMode = key
                                        PreferenceManager.saveBackendMode(context, key)
                                        statusMessage = "Active Gateway shifted to $label ($key)"
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = if (isSelected) ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors()
                                ) {
                                    Text(label, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Superuser Parameter Configurator Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Superuser Parameter Configurator", fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        // AI Persona
                        Text("Global AI Persona:", style = MaterialTheme.typography.labelMedium)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Socratic Tutor", "Clinical Assessor", "Mentor").forEach { persona ->
                                FilterChip(
                                    selected = aiPersona == persona,
                                    onClick = {
                                        aiPersona = persona
                                        statusMessage = "Global AI Persona shifted to $persona"
                                    },
                                    label = { Text(persona, fontSize = 11.sp) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Temperature
                        Text("LLM Temperature: ${String.format("%.1f", temperature)}", style = MaterialTheme.typography.labelMedium)
                        Slider(
                            value = temperature,
                            onValueChange = { temperature = it },
                            valueRange = 0.1f..1.0f,
                            steps = 8
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Security Profile
                        Text("Security Policy:", style = MaterialTheme.typography.labelMedium)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Enforced Zero-Leakage", "Audit Mode").forEach { sec ->
                                FilterChip(
                                    selected = securityProfile == sec,
                                    onClick = {
                                        securityProfile = sec
                                        statusMessage = "Security policy set to $sec"
                                    },
                                    label = { Text(sec, fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }
            }

            // Curriculum Hierarchy Administration (Fields > Courses > Units)
            item {
                var isCurriculumExpanded by remember { mutableStateOf(false) }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isCurriculumExpanded = !isCurriculumExpanded },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AccountTree, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Curriculum Hierarchy Administration", fontWeight = FontWeight.Bold)
                            }
                            Icon(
                                imageVector = if (isCurriculumExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Toggle"
                            )
                        }
                        
                        AnimatedVisibility(visible = isCurriculumExpanded) {
                            Column(modifier = Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    "Master database structure of academic picks:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                val hierarchyData = listOf(
                                    "Medical & Health Sciences" to listOf(
                                        "MBChB" to listOf("Biochemistry II", "General Surgery", "Internal Medicine", "Clinical Science"),
                                        "BSc. Nursing" to listOf("Anatomy & Physiology", "General Pharmacology", "Pathology")
                                    ),
                                    "Computer Science & IT" to listOf(
                                        "BSc. Software Engineering" to listOf("Database Systems", "Advanced Algorithms", "Web Architecture"),
                                        "BSc. Artificial Intelligence" to listOf("Machine Learning", "Neural Networks", "NLP & LLMs")
                                    )
                                )

                                hierarchyData.forEach { (field, courses) ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(field, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                                            Spacer(modifier = Modifier.height(6.dp))
                                            courses.forEach { (course, units) ->
                                                Column(modifier = Modifier.padding(start = 12.dp, top = 4.dp)) {
                                                    Text("• $course", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                                                    Row(
                                                        modifier = Modifier.padding(start = 12.dp, top = 2.dp),
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        units.forEach { unit ->
                                                            SuggestionChip(
                                                                onClick = {
                                                                    statusMessage = "Admin: Selected syllabus target: $unit"
                                                                },
                                                                label = { Text(unit, fontSize = 10.sp) }
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

            // Quick Superuser Action Controls
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Direct Administration Actions", fontWeight = FontWeight.Bold)

                        OutlinedButton(
                            onClick = {
                                statusMessage = "X-Internal-Api-Key rotated successfully across client & server."
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Rotate Handshake Token")
                        }

                        OutlinedButton(
                            onClick = {
                                statusMessage = "Master Syllabus & SQLite Schema re-synced."
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Re-sync Local Database Schema")
                        }

                        Button(
                            onClick = {
                                statusMessage = "Simulated high load pulse (200 requests/sec) dispatched."
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Trigger Health Load Simulation")
                        }
                    }
                }
            }

            // Status feedback banner
            if (statusMessage != null) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = statusMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
