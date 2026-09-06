package com.example.edu_ai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
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
import androidx.compose.ui.text.style.TextDecoration
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
    .replace(Regex("[\\u0000-\\u001F\\u007F]")) { "" }
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
            val imageMatch = imageRegex.find(trimmed)
            when {
                imageMatch != null -> {
                    flushText()
                    flushTable()
                    blocks.add(MarkdownBlock(BlockType.IMAGE, imageMatch.groupValues[2], imageMatch.groupValues[1]))
                }
                trimmed.startsWith("|") && trimmed.endsWith("|") -> {
                    flushText()
                    tableLines.add(trimmed)
                }
                trimmed.isNotBlank() -> {
                    flushTable()
                    textLines.add(rawLine)
                }
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
            .replace(headerRegex) { it.groupValues[1].trim() }
            .replace(unorderedListRegex) { "• ${it.groupValues[1].trim()}" }
            .replace(orderedListRegex) { "${it.groupValues[1]}. ${it.groupValues[2].trim()}" }
            .replace(Regex("""(?m)^\s*---+\s*$"""), "")

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

        var lastIndex = 0
        for (token in tokens) {
            if (token.range.first < lastIndex) continue
            if (token.range.first > lastIndex) append(processedText.substring(lastIndex, token.range.first))
            when (token.type) {
                "bold" -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(token.content) }
                "italic" -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(token.content) }
                "strike" -> withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) { append(token.content) }
                "code" -> withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = Color.LightGray.copy(alpha = 0.25f))) { append(token.content) }
                "link" -> {
                    pushStringAnnotation("URL", token.url.orEmpty())
                    withStyle(SpanStyle(color = Color(0xFF2563EB), fontWeight = FontWeight.Bold)) { append(token.content) }
                    pop()
                }
            }
            lastIndex = token.range.last + 1
        }
        if (lastIndex < processedText.length) append(processedText.substring(lastIndex))
    }
}

data class Token(val range: IntRange, val type: String, val content: String, val url: String? = null)
