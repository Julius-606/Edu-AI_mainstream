package com.example.edu_ai.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.edu_ai.data.model.HighlightColor
import com.example.edu_ai.data.model.TextHighlight
import com.example.edu_ai.utils.TactileFeedback
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Reusable Multi-Color Text Highlighting Toolbar.
 * Provides selection between 5 distinct clinical colors, a quick add highlight dialog,
 * and a saved highlights management bottom sheet.
 */
@Composable
fun HighlighterBar(
    selectedColor: HighlightColor,
    onColorSelected: (HighlightColor) -> Unit,
    highlightsCount: Int,
    onAddHighlightClick: () -> Unit,
    onManageHighlightsClick: () -> Unit,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
        tonalElevation = 6.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Label
            Icon(
                Icons.Default.BorderColor,
                contentDescription = null,
                tint = Color(selectedColor.bgHex),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                selectedColor.displayName,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Palette of 5 Colors
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HighlightColor.entries.forEach { colorOption ->
                    val isSelected = colorOption == selectedColor
                    val chipColor = Color(colorOption.bgHex)

                    Box(
                        modifier = Modifier
                            .size(if (isSelected) 30.dp else 24.dp)
                            .clip(CircleShape)
                            .background(chipColor)
                            .border(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.2f),
                                shape = CircleShape
                            )
                            .clickable {
                                onColorSelected(colorOption)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(colorOption.textHex),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Add highlight manually
            IconButton(
                onClick = onAddHighlightClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add Highlight",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // View & manage saved highlights
            IconButton(
                onClick = onManageHighlightsClick,
                modifier = Modifier.size(32.dp)
            ) {
                BadgedBox(badge = {
                    if (highlightsCount > 0) {
                        Badge(containerColor = MaterialTheme.colorScheme.primary) {
                            Text("$highlightsCount", fontSize = 9.sp)
                        }
                    }
                }) {
                    Icon(
                        Icons.Default.FormatListBulleted,
                        contentDescription = "Saved Highlights",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Close highlighter bar
            IconButton(
                onClick = onCloseClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close Highlighter",
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Dialog enabling the user to enter/paste any keyword, clinical fact, or phrase to highlight in their chosen color.
 */
@Composable
fun QuickAddHighlightDialog(
    initialText: String = "",
    selectedColor: HighlightColor,
    onDismiss: () -> Unit,
    onConfirm: (text: String, color: HighlightColor, note: String?) -> Unit
) {
    var highlightText by remember { mutableStateOf(initialText) }
    var currentColor by remember { mutableStateOf(selectedColor) }
    var noteText by remember { mutableStateOf("") }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.BorderColor, contentDescription = null, tint = Color(currentColor.bgHex))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Highlight Key Term", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Enter or paste the term, diagnostic finding, or clinical formula you want highlighted across this module.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline
                )

                // Input field
                OutlinedTextField(
                    value = highlightText,
                    onValueChange = { highlightText = it },
                    label = { Text("Highlighted Snippet / Term") },
                    placeholder = { Text("E.g. FEV1/FVC < 0.70 or Albuterol") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = clipboard.primaryClip
                            if (clip != null && clip.itemCount > 0) {
                                val pasteText = clip.getItemAt(0).text?.toString() ?: ""
                                if (pasteText.isNotBlank()) {
                                    highlightText = pasteText.trim()
                                }
                            }
                        }) {
                            Icon(Icons.Default.ContentPaste, contentDescription = "Paste", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                )

                // Color Selection
                Text("Category Color:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HighlightColor.entries.forEach { option ->
                        val isSelected = option == currentColor
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { currentColor = option }
                                .padding(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(option.bgHex))
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color(option.textHex),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                option.displayName.split(" ").first(),
                                fontSize = 9.sp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }

                // Optional personal note
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Personal Study Mnemonic / Note (Optional)") },
                    placeholder = { Text("E.g. High-yield distractor on Step 1...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (highlightText.isNotBlank()) {
                        TactileFeedback.triggerSubtleClick(context)
                        onConfirm(highlightText.trim(), currentColor, noteText.trim().ifEmpty { null })
                    }
                },
                enabled = highlightText.isNotBlank()
            ) {
                Text("Save Highlight")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Bottom Sheet displaying all saved highlights, categorized by color, with options to filter, copy, or delete.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageHighlightsBottomSheet(
    highlights: List<TextHighlight>,
    onDeleteHighlight: (String) -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf<HighlightColor?>(null) }
    var showConfirmClear by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val filteredHighlights = remember(highlights, selectedFilter) {
        if (selectedFilter == null) highlights
        else highlights.filter { it.colorHex.equals(selectedFilter?.hex, ignoreCase = true) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.BorderColor,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Study Highlights (${highlights.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (highlights.isNotEmpty()) {
                    TextButton(onClick = { showConfirmClear = true }) {
                        Text("Clear All", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Color Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedFilter == null,
                        onClick = { selectedFilter = null },
                        label = { Text("All", fontSize = 11.sp) }
                    )
                }
                items(HighlightColor.entries) { colorOption ->
                    FilterChip(
                        selected = selectedFilter == colorOption,
                        onClick = {
                            selectedFilter = if (selectedFilter == colorOption) null else colorOption
                        },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(colorOption.bgHex))
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(colorOption.displayName, fontSize = 11.sp)
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredHighlights.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.FormatQuote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "No highlights in this category yet.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredHighlights, key = { it.id }) { item ->
                        val parsedColor = try {
                            Color(android.graphics.Color.parseColor(item.colorHex))
                        } catch (e: Exception) {
                            Color(0xFFFEF08A)
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            border = BorderStroke(1.dp, parsedColor.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(parsedColor)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "\"${item.text}\"",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (item.note != null) {
                                        Text(
                                            text = item.note,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = item.label + " • " + SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(item.timestamp)),
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }

                                IconButton(onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Highlight", item.text)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                }) {
                                    Icon(
                                        Icons.Default.ContentCopy,
                                        contentDescription = "Copy",
                                        tint = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                IconButton(onClick = { onDeleteHighlight(item.id) }) {
                                    Icon(
                                        Icons.Default.DeleteOutline,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showConfirmClear) {
        AlertDialog(
            onDismissRequest = { showConfirmClear = false },
            title = { Text("Clear All Highlights?") },
            text = { Text("This will permanently remove all highlighted phrases for this section.") },
            confirmButton = {
                TextButton(onClick = {
                    onClearAll()
                    showConfirmClear = false
                }) {
                    Text("CLEAR ALL", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmClear = false }) {
                    Text("CANCEL")
                }
            }
        )
    }
}
