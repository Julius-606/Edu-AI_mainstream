package com.example.edu_ai.ui.screens.student

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.edu_ai.data.local.BookmarkEntity
import com.example.edu_ai.ui.components.DynamicBackground
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksScreen(
    userId: String,
    onBack: () -> Unit,
    onNavigateToLearn: (Long) -> Unit,
    onNavigateToBrowser: (String) -> Unit,
    studentViewModel: StudentViewModel = viewModel(factory = StudentViewModel.Factory)
) {
    val bookmarks by studentViewModel.bookmarks.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("All Clips", "Syllabus", "Browser")

    val filteredBookmarks = remember(bookmarks, selectedTab) {
        when (selectedTab) {
            1 -> bookmarks.filter { it.type == "learn" }
            2 -> bookmarks.filter { it.type == "browser" }
            else -> bookmarks
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        DynamicBackground()

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Saved Bookmarks & Clips",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp
                        )
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
                // Filter tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                        )
                    }
                }

                if (filteredBookmarks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Bookmark,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                "No clips found here yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredBookmarks, key = { it.id }) { bookmark ->
                            BookmarkRowCard(
                                bookmark = bookmark,
                                onDelete = {
                                    scope.launch {
                                        studentViewModel.deleteBookmark(bookmark.id)
                                    }
                                },
                                onStudyNow = {
                                    if (bookmark.type == "learn") {
                                        bookmark.target.toLongOrNull()?.let { onNavigateToLearn(it) }
                                    } else {
                                        onNavigateToBrowser(bookmark.target)
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
fun BookmarkRowCard(
    bookmark: BookmarkEntity,
    onDelete: () -> Unit,
    onStudyNow: () -> Unit
) {
    val dateStr = remember(bookmark.timestamp) {
        try {
            val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            sdf.format(Date(bookmark.timestamp))
        } catch (e: Exception) {
            "Just now"
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (bookmark.type == "learn") Icons.Default.MenuBook else Icons.Default.OpenInBrowser,
                        contentDescription = null,
                        tint = if (bookmark.type == "learn") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = bookmark.title,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = bookmark.context,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (!bookmark.notes.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                    )
                ) {
                    Text(
                        text = "📝 Notes: ${bookmark.notes}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Saved $dateStr",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Button(
                    onClick = onStudyNow,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.height(30.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (bookmark.type == "learn") "Study Now" else "Open Link",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
