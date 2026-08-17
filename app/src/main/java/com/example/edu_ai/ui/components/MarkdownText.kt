package com.example.edu_ai.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun FormattedText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    onLinkClick: ((String) -> Unit)? = null
) {
    val blocks = remember(text) { splitIntoBlocks(text) }
    
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        blocks.forEach { block ->
            when (block.type) {
                BlockType.TABLE -> TableBlock(block.content)
                BlockType.IMAGE -> ImageBlock(block.content, block.extra)
                else -> {
                    val annotatedString = parseMarkdown(block.content)
                    androidx.compose.foundation.text.ClickableText(
                        text = annotatedString,
                        style = style,
                        maxLines = maxLines,
                        overflow = overflow,
                        onClick = { offset ->
                            annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                                .firstOrNull()?.let { annotation ->
                                    onLinkClick?.invoke(annotation.item)
                                }
                        }
                    )
                }
            }
        }
    }
}

enum class BlockType { TEXT, TABLE, IMAGE }
data class MarkdownBlock(val type: BlockType, val content: String, val extra: String? = null)

fun splitIntoBlocks(text: String): List<MarkdownBlock> {
    val lines = text.lines()
    val blocks = mutableListOf<MarkdownBlock>()
    var currentTable = mutableListOf<String>()
    
    val imageRegex = Regex("""^!\[(.*?)]\((.*?)\)$""")

    for (line in lines) {
        val trimmed = line.trim()
        
        // Check for Image
        val imageMatch = imageRegex.find(trimmed)
        if (imageMatch != null) {
            if (currentTable.isNotEmpty()) {
                blocks.add(MarkdownBlock(BlockType.TABLE, currentTable.joinToString("\n")))
                currentTable = mutableListOf()
            }
            blocks.add(MarkdownBlock(BlockType.IMAGE, imageMatch.groupValues[2], imageMatch.groupValues[1]))
            continue
        }

        // Check for Table
        if (trimmed.startsWith("|") && trimmed.endsWith("|")) {
            currentTable.add(line)
        } else {
            if (currentTable.isNotEmpty()) {
                blocks.add(MarkdownBlock(BlockType.TABLE, currentTable.joinToString("\n")))
                currentTable = mutableListOf()
            }
            if (line.isNotBlank()) {
                blocks.add(MarkdownBlock(BlockType.TEXT, line))
            }
        }
    }
    if (currentTable.isNotEmpty()) {
        blocks.add(MarkdownBlock(BlockType.TABLE, currentTable.joinToString("\n")))
    }
    return blocks
}

@Composable
fun TableBlock(content: String) {
    val rows = content.lines().filter { it.contains("|") && !it.contains("---") }
    Column(modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)).padding(8.dp)) {
        rows.forEachIndexed { index, row ->
            val cells = row.split("|").filter { it.isNotBlank() }.map { it.trim() }
            Row(modifier = Modifier.fillMaxWidth()) {
                cells.forEach { cell ->
                    Box(modifier = Modifier.weight(1f).padding(4.dp)) {
                        Text(
                            text = cell,
                            style = if (index == 0) MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold) else MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            if (index < rows.size - 1) HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        }
    }
}

@Composable
fun ImageBlock(url: String, alt: String?) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = url,
            contentDescription = alt,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 300.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Fit
        )
        if (!alt.isNullOrBlank()) {
            Text(
                text = alt,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

/**
 * A simple Markdown parser for Compose.
 */
fun parseMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        val boldRegex = Regex("""\*\*(.*?)\*\*""", RegexOption.DOT_MATCHES_ALL)
        val italicRegex = Regex("""\*(.*?)\*""", RegexOption.DOT_MATCHES_ALL)
        val codeRegex = Regex("""`(.*?)`""", RegexOption.DOT_MATCHES_ALL)
        val linkRegex = Regex("""\[(.*?)]\((.*?)\)""", RegexOption.DOT_MATCHES_ALL)
        val headerRegex = Regex("""^#+\s*(.*)$""", RegexOption.MULTILINE)
        val listRegex = Regex("""^\s*[-*+]\s+(.*)$""", RegexOption.MULTILINE)

        var processedText = text
            .replace(headerRegex) { it.groupValues[1] }
            .replace(listRegex) { "• ${it.groupValues[1]}" }

        val tokens = mutableListOf<Token>()
        
        boldRegex.findAll(processedText).forEach { tokens.add(Token(it.range, "bold", it.groupValues[1])) }
        italicRegex.findAll(processedText).forEach { match ->
            if (tokens.none { it.range.contains(match.range.first) }) {
                tokens.add(Token(match.range, "italic", match.groupValues[1]))
            }
        }
        codeRegex.findAll(processedText).forEach { tokens.add(Token(it.range, "code", it.groupValues[1])) }
        linkRegex.findAll(processedText).forEach { tokens.add(Token(it.range, "link", it.groupValues[1], it.groupValues[2])) }
        
        tokens.sortBy { it.range.first }
        
        var lastIndex = 0
        for (token in tokens) {
            if (token.range.first > lastIndex) {
                append(processedText.substring(lastIndex, token.range.first))
            }
            
            when (token.type) {
                "bold" -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(token.content) }
                "italic" -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(token.content) }
                "code" -> withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = Color.LightGray.copy(alpha = 0.3f))) { append(token.content) }
                "link" -> {
                    pushStringAnnotation(tag = "URL", annotation = token.url ?: "")
                    withStyle(SpanStyle(color = Color.Blue, fontWeight = FontWeight.Bold)) { append(token.content) }
                    pop()
                }
            }
            lastIndex = token.range.last + 1
        }
        
        if (lastIndex < processedText.length) {
            append(processedText.substring(lastIndex))
        }
    }
}

data class Token(val range: IntRange, val type: String, val content: String, val url: String? = null)
