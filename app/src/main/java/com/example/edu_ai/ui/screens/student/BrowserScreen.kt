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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.edu_ai.ui.components.DynamicBackground
import com.example.edu_ai.ui.components.FormattedText
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    userId: String,
    url: String,
    onBack: () -> Unit,
    studentViewModel: StudentViewModel = viewModel(factory = StudentViewModel.Factory)
) {
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val context = androidx.compose.ui.platform.LocalContext.current

    var activeUrl by remember { mutableStateOf(url) }
    var bookmarkNote by remember { mutableStateOf("") }
    var showBookmarkDialog by remember { mutableStateOf(false) }
    var isBookmarked by remember { mutableStateOf(false) }

    val localBookmarks by studentViewModel.bookmarks.collectAsState(initial = emptyList())
    LaunchedEffect(localBookmarks, activeUrl) {
        isBookmarked = localBookmarks.any { it.target == activeUrl && it.type == "browser" }
    }

    // Dynamic Clinical Web Contents
    val webPageTitle = remember(activeUrl) {
        if (activeUrl.contains("hemolytic_anemia", true)) {
            "StatPearls - Hemolytic Anemia Pathophysiology"
        } else {
            "National Center for Biotechnology Information (NCBI) • Glycolysis"
        }
    }

    val webPageContent = remember(activeUrl) {
        if (activeUrl.contains("hemolytic_anemia", true)) {
            """
            # StatPearls: Hemolytic Anemia Clinical Guidelines
            **Authors:** Dr. Sarah Jenkins, MD; Dr. Amit Patel, FACP.
            **Updated:** September 2026.

            ## Introduction
            Hemolytic anemia is defined as the premature destruction of erythrocytes (red blood cells, RBCs) before their normal lifespan of 120 days. Hemolysis can occur intravascularly or extravascularly within the reticuloendothelial system (primarily the spleen).

            ## Etiology & Enzyme Defects
            Inherited enzyme deficiencies are primary causes of extravascular hemolysis:
            1. **Pyruvate Kinase Deficiency:** Impairs ATP generation via glycolysis, causing RBC deformity and premature destruction.
            2. **Phosphofructokinase Deficiency (Tarui Disease):** Affects glycolysis, leading to myogenic symptoms alongside hemolysis.
            
            ## Clinical Symptoms
            * Pale sclera, jaundice, and marked splenomegaly.
            * Exertional fatigue, tachycardia, and dark urine (haemosiderinuria).

            ## Treatment Protocols
            * Hydration and avoidance of oxidant stressors.
            * Folic acid supplementation to support active erythropoiesis.
            * Splenectomy in severe refractory extravascular cases.
            """.trimIndent()
        } else {
            """
            # NCBI PMC: Regulation of Erythrocyte Glycolysis
            **Journal of Hematology Research** • PMC628391
            
            ## Abstract
            Unlike nucleated cells, mature mammalian erythrocytes lack mitochondria and rely exclusively on anaerobic glycolysis for ATP synthesis. Glycolysis generates ATP to drive the Na+/K+-ATPase, which maintains osmotic equilibrium and prevents swelling.

            ## Key Regulatory Checkpoints
            * **Hexokinase:** The initial step, inhibited by glucose-6-phosphate.
            * **Phosphofructokinase-1 (PFK-1):** The rate-limiting enzyme. Regulated dynamically by cytosolic pH, AMP, and allosteric activators. Deficiencies result in Tarui's Syndrome, with hemolytic crises aggravated by strenuous muscular work.
            * **Pyruvate Kinase:** Converts phosphoenolpyruvate to pyruvate, generating ATP. Deficiencies cause high 2,3-BPG levels, shifting the oxygen-haemoglobin dissociation curve.
            """.trimIndent()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        DynamicBackground()

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Column {
                    // Browser Top Control Panel
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Trace In-App Browser", fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    // URL Address Bar with Bookmark Action
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = activeUrl,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )

                                IconButton(
                                    onClick = {
                                        com.example.edu_ai.utils.TactileFeedback.triggerSubtleClick(context)
                                        if (isBookmarked) {
                                            scope.launch {
                                                val bm = localBookmarks.find { it.target == activeUrl && it.type == "browser" }
                                                if (bm != null) {
                                                    studentViewModel.deleteBookmark(bm.id)
                                                }
                                            }
                                        } else {
                                            showBookmarkDialog = true
                                        }
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                        contentDescription = "Bookmark Link",
                                        tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = {
                                com.example.edu_ai.utils.TactileFeedback.triggerSubtleClick(context)
                                /* Refresh page */
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(scrollState)
                    .background(Color.White) // Authentic web-page white backdrop
                    .padding(18.dp)
            ) {
                Text(
                    text = webPageTitle,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1E293B),
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 2.dp)
                Spacer(modifier = Modifier.height(14.dp))

                FormattedText(
                    text = webPageContent,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(30.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            "📚 Related Research Quicklinks",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "StatPearls Haemolytic Anemia Guidelines",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .clickable { activeUrl = "https://statpearls.com/articles/hemolytic_anemia" }
                                .padding(vertical = 4.dp)
                        )
                        Text(
                            "NCBI Biochemistry Glycolysis PMC Paper",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .clickable { activeUrl = "https://ncbi.nlm.nih.gov/pmc/glycolysis" }
                                .padding(vertical = 4.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    // Bookmark Dialog
    if (showBookmarkDialog) {
        AlertDialog(
            onDismissRequest = { showBookmarkDialog = false },
            title = { Text("Save Browser Bookmark") },
            text = {
                Column {
                    Text("Add search annotations to this medical research clip.", fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = bookmarkNote,
                        onValueChange = { bookmarkNote = it },
                        placeholder = { Text("E.g., high-yield clinical guideline notes for RBC destruction...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        studentViewModel.addBookmark(
                            userId = userId,
                            type = "browser",
                            title = webPageTitle,
                            target = activeUrl,
                            context = "Reference Link: $activeUrl",
                            notes = bookmarkNote.trim().ifEmpty { null }
                        )
                        isBookmarked = true
                        showBookmarkDialog = false
                        bookmarkNote = ""
                    }
                }) {
                    Text("Save Bookmark")
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
