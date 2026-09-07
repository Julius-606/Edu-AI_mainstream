package com.example.edu_ai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val linkPreviews = remember(text) { extractLinks(text) }
    val resolvedStyle = if (style.color == Color.Unspecified) {
        style.copy(color = MaterialTheme.colorScheme.onSurface)
    } else {
        style
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        blocks.forEach { block ->
            when (block.type) {
                BlockType.TABLE -> TableBlock(block.content, resolvedStyle, onLinkClick)
                BlockType.IMAGE -> ImageBlock(block.content, block.extra)
                BlockType.CODE -> CodeBlock(block.content, resolvedStyle)
                BlockType.TEXT -> {
                    val annotatedString = parseMarkdown(block.content)
                    ClickableText(
                        text = annotatedString,
                        style = resolvedStyle,
                        maxLines = maxLines,
                        overflow = overflow,
                        onClick = { offset ->
                            annotatedString.getStringAnnotations("URL", offset, offset)
                                .firstOrNull()?.let { onLinkClick?.invoke(it.item) }
                        }
                    )
                }
            }
            linkPreviews.forEach { url ->
                LinkPreview(url) { onLinkClick?.invoke(url) }
            }
        }
    }
}

enum class BlockType { TEXT, TABLE, IMAGE, CODE }
data class MarkdownBlock(val type: BlockType, val content: String, val extra: String? = null)

private fun sanitizeMarkdownText(text: String): String = text
    .replace("\r\n", "\n")
    .replace("\r", "\n")
    .replace("\u200B", "")
    .replace("\u00A0", " ")
    .replace("\uFEFF", "")
    // Keep line feeds and tabs: they are meaningful Markdown structure.
    .replace(Regex("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F\\u007F]")) { "" }
    .trim()

fun splitIntoBlocks(text: String): List<MarkdownBlock> {
    val normalized = sanitizeMarkdownText(text)
    if (normalized.isBlank()) return emptyList()

    val blocks = mutableListOf<MarkdownBlock>()
    val tableLines = mutableListOf<String>()
    val textLines = mutableListOf<String>()
    val codeLines = mutableListOf<String>()
    var inCodeBlock = false
    val imageRegex = Regex("""^\s*!\[(.*?)]\((https?://[^\s)]+|/[^\s)]+|[^\s)]+)\)\s*$""", RegexOption.IGNORE_CASE)

    fun flushText() {
        if (textLines.isNotEmpty()) {
            blocks.add(MarkdownBlock(BlockType.TEXT, textLines.joinToString("\n")))
            textLines.clear()
        }
    }

    fun flushTable() {
        if (tableLines.isNotEmpty()) {
            blocks.add(MarkdownBlock(BlockType.TABLE, tableLines.joinToString("\n")))
            tableLines.clear()
        }
    }

    normalized.lines().forEach { rawLine ->
        val trimmed = rawLine.trim()
        if (trimmed.startsWith("```")) {
            if (inCodeBlock) {
                blocks.add(MarkdownBlock(BlockType.CODE, codeLines.joinToString("\n")))
                codeLines.clear()
            } else {
                flushText()
                flushTable()
            }
            inCodeBlock = !inCodeBlock
        } else if (inCodeBlock) {
            codeLines.add(rawLine)
        } else {
            // Identify images
            val imageMatch = imageRegex.find(trimmed)
            if (imageMatch != null) {
                flushText()
                flushTable()
                blocks.add(MarkdownBlock(BlockType.IMAGE, imageMatch.groupValues[2], imageMatch.groupValues[1]))
            } else if (trimmed.count { it == '|' } >= 2) {
                flushText()
                tableLines.add(trimmed)
            } else {
                // If it's not a table or image, it's text.
                // We keep the structure.
                flushTable()
                textLines.add(rawLine)
            }
        }
    }

    flushText()
    flushTable()
    if (inCodeBlock && codeLines.isNotEmpty()) {
        blocks.add(MarkdownBlock(BlockType.CODE, codeLines.joinToString("\n")))
    }
    return blocks
}

@Composable
private fun CodeBlock(content: String, style: TextStyle) {
    Text(
        text = content,
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.LightGray.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
            .padding(12.dp),
        style = style.copy(fontFamily = FontFamily.Monospace),
        softWrap = true
    )
}

@Composable
private fun TableBlock(content: String, style: TextStyle, onLinkClick: ((String) -> Unit)?) {
    val rows = content.lines().filterNot { row ->
        row.split("|")
            .map { it.trim().removePrefix(":").removeSuffix(":") }
            .filter { it.isNotEmpty() }
            .all { it.length >= 3 && it.all { character -> character == '-' } }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        rows.forEachIndexed { index, row ->
            val cells = row.split("|").dropWhile { it.isBlank() }.dropLastWhile { it.isBlank() }.map { it.trim() }
            Row(modifier = Modifier.fillMaxWidth()) {
                cells.forEach { cell ->
                    val parsed = parseMarkdown(cell)
                    Box(modifier = Modifier.weight(1f).padding(4.dp)) {
                        ClickableText(
                            text = parsed,
                            style = style.copy(
                                fontWeight = if (index == 0) FontWeight.Bold else style.fontWeight
                            ),
                            onClick = { offset ->
                                parsed.getStringAnnotations("URL", offset, offset)
                                    .firstOrNull()?.let { onLinkClick?.invoke(it.item) }
                            }
                        )
                    }
                }
            }
            if (index < rows.lastIndex) HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        }
    }
}

@Composable
private fun ImageBlock(url: String, alt: String?) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
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

private fun extractLinks(text: String): List<String> {
    val markdownLinks = Regex("""\[[^\]]+]\((https?://[^\s)]+)\)""")
    val bareLinks = Regex("""https?://[^\s)]+""")
    return (markdownLinks.findAll(text).map { it.groupValues[1] } +
        bareLinks.findAll(text).map { it.value.trimEnd('.', ',', ';') })
        .distinct()
}

@Composable
private fun LinkPreview(url: String, onClick: () -> Unit) {
    val host = java.net.URI.create(url).host ?: url
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = if (host.contains("youtube", ignoreCase = true) || host == "youtu.be") {
                    "YouTube video"
                } else {
                    "Recommended resource"
                },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            Text(text = host, style = MaterialTheme.typography.bodySmall)
            Text(
                text = url,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

data class Token(val range: IntRange, val type: String, val content: String, val url: String? = null)
data class RenderMarker(
    val range: IntRange,
    val type: String,
    val content: String,
    val url: String? = null
)

fun parseMarkdown(text: String): AnnotatedString {
    val sanitized = sanitizeMarkdownText(text)
    if (sanitized.isBlank()) return AnnotatedString("")

    return buildAnnotatedString {
        val boldRegex = Regex("""(?<!\*)\*\*(.+?)(?<!\\)\*\*(?!\*)""", RegexOption.DOT_MATCHES_ALL)
        val italicRegex = Regex("""(?<!\*)\*(?!\*)(.+?)(?<!\*)\*(?!\*)""", RegexOption.DOT_MATCHES_ALL)
        val strikeRegex = Regex("""~~(.+?)~~""", RegexOption.DOT_MATCHES_ALL)
        val codeRegex = Regex("""`([^`\n]+)`""")
        val linkRegex = Regex("""(?<!\!)\[([^\]]+)]\((https?://[^\s)]+|/[^\s)]+|[^\s)]+)\)""")
        val headerRegex = Regex("""(?m)^#{1,6}\s*(.+?)\s*#*\s*$""")
        val unorderedListRegex = Regex("""(?m)^\s*[-*+]\s+(.*)$""")
        val orderedListRegex = Regex("""(?m)^\s*(\d+)[.)]\s+(.*)$""")

        val processedText = sanitized
            .replace(Regex("""(?m)(-{3,})\s*(#{1,6})"""), "\n$2")
            .replace(Regex("""(?m)^\s*-{3,}\s*$"""), "")
            .replace(Regex("""(?m)^\s*>\s?"""), "")
            .replace(Regex("""(?i)<br\s*/?>"""), "\n")
            .replace(headerRegex) { match ->
                val level = match.value.takeWhile { it == '#' }.length
                "\n" + "HEADER_LVL_${level}_START" + match.groupValues[1].trim() + "HEADER_END" + "\n"
            }
            .replace(unorderedListRegex) { "\n  • ${it.groupValues[1].trim()}" }
            .replace(orderedListRegex) { "\n  ${it.groupValues[1]}. ${it.groupValues[2].trim()}" }
        val tokens = mutableListOf<Token>()
        boldRegex.findAll(processedText).forEach { tokens.add(Token(it.range, "bold", it.groupValues[1])) }
        italicRegex.findAll(processedText).forEach { match ->
            if (tokens.none { it.range.first <= match.range.first && match.range.last <= it.range.last }) {
                tokens.add(Token(match.range, "italic", match.groupValues[1]))
            }
        }
        strikeRegex.findAll(processedText).forEach { tokens.add(Token(it.range, "strike", it.groupValues[1])) }
        codeRegex.findAll(processedText).forEach { tokens.add(Token(it.range, "code", it.groupValues[1])) }
        linkRegex.findAll(processedText).forEach { tokens.add(Token(it.range, "link", it.groupValues[1], it.groupValues[2])) }
        tokens.sortBy { it.range.first }

        val headerMatchRegex = Regex("""HEADER_LVL_(\d)_START(.+?)HEADER_END""")
        val allMarkers = mutableListOf<RenderMarker>()
        tokens.forEach { allMarkers.add(RenderMarker(it.range, it.type, it.content, it.url)) }
        headerMatchRegex.findAll(processedText).forEach {
            allMarkers.add(RenderMarker(it.range, "header_${it.groupValues[1]}", it.groupValues[2]))
        }
        allMarkers.sortBy { it.range.first }
        
        var currentPos = 0
        for (marker in allMarkers) {
            if (marker.range.first < currentPos) continue
            if (marker.range.first > currentPos) {
                append(processedText.substring(currentPos, marker.range.first))
            }
            
            when {
                marker.type == "bold" -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(marker.content) }
                marker.type == "italic" -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(marker.content) }
                marker.type.startsWith("header_") -> {
                    val level = marker.type.substringAfter("_").toIntOrNull() ?: 1
                    val size = when(level) {
                        1 -> 24.sp
                        2 -> 20.sp
                        else -> 18.sp
                    }
                    withStyle(SpanStyle(fontSize = size, fontWeight = FontWeight.ExtraBold)) { append(marker.content) }
                }
                marker.type == "link" -> {
                    pushStringAnnotation("URL", marker.url ?: "")
                    withStyle(SpanStyle(color = Color(0xFF2563EB), textDecoration = TextDecoration.Underline)) { append(marker.content) }
                    pop()
                }
                marker.type == "code" -> withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = Color.LightGray.copy(alpha = 0.25f))) { append(marker.content) }
                marker.type == "strike" -> withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) { append(marker.content) }
            }
            currentPos = marker.range.last + 1
        }
        if (currentPos < processedText.length) append(processedText.substring(currentPos))
    }
}
