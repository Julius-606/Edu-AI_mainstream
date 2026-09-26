package com.example.edu_ai.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.regex.Pattern

/**
 * Enhanced Markdown and Interactive Academic Formatted Text renderer.
 * Features:
 * 1. Eye-catchy Flowcharts & Process Diagrams rendering (from Mermaid, ASCII diagrams, or arrow blocks).
 * 2. Accommodating Markdown Tables with horizontal scrolling for large datasets.
 * 3. Highlights (==text==), Smart Color Tags, and Callout Cards.
 * 4. Interactive YouTube Video and Web Reference Recommendation Cards.
 */
@Composable
fun FormattedText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    onLinkClicked: ((String) -> Unit)? = null
) {
    val blocks = parseMarkdownBlocks(text)
    val context = LocalContext.current

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        for (block in blocks) {
            when (block) {
                is ContentBlock.Table -> {
                    MarkdownTableView(table = block)
                }
                is ContentBlock.FlowDiagram -> {
                    FlowDiagramView(diagram = block)
                }
                is ContentBlock.Callout -> {
                    CalloutCard(callout = block)
                }
                is ContentBlock.YouTubeVideo -> {
                    VideoRecommendationCard(
                        title = block.title,
                        url = block.url,
                        onOpen = {
                            if (onLinkClicked != null) onLinkClicked(block.url)
                            else {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(block.url))
                                context.startActivity(intent)
                            }
                        }
                    )
                }
                is ContentBlock.WebReference -> {
                    WebReferenceCard(
                        title = block.title,
                        url = block.url,
                        onOpen = {
                            if (onLinkClicked != null) onLinkClicked(block.url)
                            else {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(block.url))
                                context.startActivity(intent)
                            }
                        }
                    )
                }
                is ContentBlock.Paragraph -> {
                    val annotated = parseInlineMarkdown(block.text)
                    androidx.compose.foundation.text.selection.SelectionContainer {
                        Text(
                            text = annotated,
                            style = style,
                            maxLines = maxLines,
                            overflow = overflow
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------
// CONTENT BLOCKS
// ---------------------------------------------------------

sealed class ContentBlock {
    data class Paragraph(val text: String) : ContentBlock()
    data class Table(val headers: List<String>, val rows: List<List<String>>) : ContentBlock()
    data class FlowDiagram(val title: String, val steps: List<FlowStep>) : ContentBlock()
    data class Callout(val type: CalloutType, val title: String, val message: String) : ContentBlock()
    data class YouTubeVideo(val title: String, val url: String) : ContentBlock()
    data class WebReference(val title: String, val url: String) : ContentBlock()
}

data class FlowStep(
    val id: String,
    val title: String,
    val description: String? = null,
    val isDecision: Boolean = false,
    val tag: String? = null
)

enum class CalloutType { NOTE, TIP, WARNING, CLINICAL }

// ---------------------------------------------------------
// BLOCK PARSER
// ---------------------------------------------------------

fun parseMarkdownBlocks(rawText: String): List<ContentBlock> {
    val blocks = mutableListOf<ContentBlock>()
    val lines = rawText.lines()
    var i = 0

    while (i < lines.size) {
        val line = lines[i]

        // 1. Check for Code Block that is a Flowchart / Diagram (e.g. ```mermaid, ```flowchart, or diagram arrows)
        if (line.trim().startsWith("```mermaid") || line.trim().startsWith("```flowchart") || line.trim().startsWith("```graph") || line.trim().startsWith("```diagram")) {
            val diagramLines = mutableListOf<String>()
            i++
            while (i < lines.size && !lines[i].trim().startsWith("```")) {
                diagramLines.add(lines[i])
                i++
            }
            if (i < lines.size) i++ // skip closing ```
            val flowDiagram = parseFlowchartBlock(diagramLines)
            blocks.add(flowDiagram)
            continue
        }

        // 2. Check for code block that has arrow symbols inside
        if (line.trim().startsWith("```")) {
            val codeLines = mutableListOf<String>()
            i++
            while (i < lines.size && !lines[i].trim().startsWith("```")) {
                codeLines.add(lines[i])
                i++
            }
            if (i < lines.size) i++ // skip closing ```
            val joined = codeLines.joinToString("\n")
            if (joined.contains("->") || joined.contains("-->") || joined.contains("==>")) {
                blocks.add(parseFlowchartBlock(codeLines))
            } else {
                blocks.add(ContentBlock.Paragraph("```\n$joined\n```"))
            }
            continue
        }

        // 3. Check for Markdown Table (at least 2 pipe lines, second line is separator like |---|)
        if (line.trim().startsWith("|") && line.trim().endsWith("|") && i + 1 < lines.size && lines[i + 1].contains("---")) {
            val tableLines = mutableListOf<String>()
            while (i < lines.size && lines[i].trim().startsWith("|") && lines[i].trim().endsWith("|")) {
                tableLines.add(lines[i].trim())
                i++
            }
            val table = parseMarkdownTable(tableLines)
            if (table != null) {
                blocks.add(table)
            } else {
                tableLines.forEach { blocks.add(ContentBlock.Paragraph(it)) }
            }
            continue
        }

        // 4. Check for Callouts (> [!NOTE], > [!TIP], > [!WARNING], > [!CLINICAL])
        if (line.trim().startsWith("> [!")) {
            val typeStr = line.substringAfter("> [!").substringBefore("]").uppercase()
            val calloutType = when (typeStr) {
                "WARNING", "CAUTION" -> CalloutType.WARNING
                "TIP", "SUCCESS" -> CalloutType.TIP
                "CLINICAL", "MED" -> CalloutType.CLINICAL
                else -> CalloutType.NOTE
            }
            val title = line.substringAfter("]", "").trim().ifBlank { typeStr }
            val messageLines = mutableListOf<String>()
            i++
            while (i < lines.size && lines[i].trim().startsWith(">")) {
                messageLines.add(lines[i].trim().removePrefix(">").trim())
                i++
            }
            blocks.add(ContentBlock.Callout(calloutType, title, messageLines.joinToString("\n")))
            continue
        }

        // 5. Check for standalone YouTube URL or [Video Title](youtube_url)
        val ytPattern = "(https?://(?:www\\.)?youtube\\.com/watch\\?v=[\\w-]+|https?://youtu\\.be/[\\w-]+)"
        val ytMatcher = Pattern.compile(ytPattern).matcher(line)
        if (ytMatcher.find()) {
            val url = ytMatcher.group(0) ?: ""
            if (url.isNotEmpty()) {
                val title = if (line.contains("[") && line.contains("]($url)")) {
                    line.substringAfter("[").substringBefore("]")
                } else {
                    "Recommended Video Lesson"
                }
                blocks.add(ContentBlock.YouTubeVideo(title = title, url = url))
                i++
                continue
            }
        }

        // 6. Check for standalone Web reference or link recommendation
        if (line.trim().startsWith("[Reference]") || line.trim().startsWith("[Web]") || (line.trim().startsWith("http") && !line.contains("youtube"))) {
            val url = if (line.trim().startsWith("http")) line.trim() else line.substringAfter("(").substringBefore(")")
            val title = if (line.contains("[") && line.contains("]")) line.substringAfter("[").substringBefore("]") else "Reference Resource"
            blocks.add(ContentBlock.WebReference(title = title, url = url))
            i++
            continue
        }

        // Default Paragraph accumulation
        val paraLines = mutableListOf<String>()
        while (i < lines.size) {
            val cur = lines[i]
            if (cur.trim().startsWith("```") || 
                (cur.trim().startsWith("|") && cur.trim().endsWith("|") && i + 1 < lines.size && lines[i + 1].contains("---")) ||
                cur.trim().startsWith("> [!") ||
                Pattern.compile(ytPattern).matcher(cur).find()) {
                break
            }
            paraLines.add(cur)
            i++
        }
        val textContent = paraLines.joinToString("\n").trim()
        if (textContent.isNotBlank()) {
            blocks.add(ContentBlock.Paragraph(textContent))
        }
    }

    return blocks
}

// ---------------------------------------------------------
// TABLE PARSER
// ---------------------------------------------------------

fun parseMarkdownTable(lines: List<String>): ContentBlock.Table? {
    if (lines.size < 2) return null
    val headerLine = lines[0]
    val headers = headerLine.split("|").map { it.trim() }.filter { it.isNotEmpty() }
    val rows = mutableListOf<List<String>>()

    for (k in 1 until lines.size) {
        val rowLine = lines[k]
        // skip separator line like |---|---|
        if (rowLine.replace("|", "").replace("-", "").replace(":", "").trim().isEmpty()) {
            continue
        }
        val cells = rowLine.split("|").map { it.trim() }
        // filter out leading/trailing empty strings resulting from leading/trailing pipes
        val validCells = if (cells.isNotEmpty() && cells.first().isEmpty() && cells.last().isEmpty()) {
            cells.subList(1, cells.size - 1)
        } else {
            cells.filter { it.isNotEmpty() }
        }
        if (validCells.isNotEmpty()) {
            rows.add(validCells)
        }
    }

    return ContentBlock.Table(headers = headers, rows = rows)
}

// ---------------------------------------------------------
// FLOWCHART PARSER
// ---------------------------------------------------------

fun parseFlowchartBlock(lines: List<String>): ContentBlock.FlowDiagram {
    val steps = mutableListOf<FlowStep>()
    var title = "Clinical Flow & Protocol Diagram"

    for (line in lines) {
        val trimmed = line.trim()
        if (trimmed.isEmpty() || trimmed.startsWith("graph") || trimmed.startsWith("flowchart")) continue

        // Check if there's a diagram title comment or label
        if (trimmed.startsWith("%%") || trimmed.startsWith("//") || trimmed.startsWith("#")) {
            title = trimmed.removePrefix("%%").removePrefix("//").removePrefix("#").trim()
            continue
        }

        // Clean out arrow transitions like A --> B or A -> B
        val arrowDelimiters = listOf("-->", "->", "==>", "-->|", "|-->")
        var matched = false
        for (delim in arrowDelimiters) {
            if (trimmed.contains(delim)) {
                val parts = trimmed.split(delim).map { it.trim() }
                parts.forEachIndexed { idx, part ->
                    val cleanPart = cleanNodeText(part)
                    if (cleanPart.isNotBlank() && steps.none { it.title.equals(cleanPart, ignoreCase = true) }) {
                        val isDecision = part.contains("{") || part.contains("?")
                        steps.add(
                            FlowStep(
                                id = "step_${steps.size + 1}",
                                title = cleanPart,
                                isDecision = isDecision,
                                tag = if (idx == 0 && steps.isEmpty()) "START" else if (idx == parts.size - 1) "ACTION" else null
                            )
                        )
                    }
                }
                matched = true
                break
            }
        }

        if (!matched) {
            val clean = cleanNodeText(trimmed)
            if (clean.isNotBlank() && steps.none { it.title.equals(clean, ignoreCase = true) }) {
                steps.add(
                    FlowStep(
                        id = "step_${steps.size + 1}",
                        title = clean,
                        isDecision = trimmed.contains("{") || trimmed.contains("?")
                    )
                )
            }
        }
    }

    if (steps.isEmpty()) {
        steps.add(FlowStep("1", "Initial Assessment", "Evaluate baseline parameters"))
        steps.add(FlowStep("2", "Diagnostic Investigation", "Order relevant labs & panels"))
        steps.add(FlowStep("3", "Targeted Protocol", "Initiate specialized intervention"))
    }

    return ContentBlock.FlowDiagram(title = title, steps = steps)
}

fun cleanNodeText(raw: String): String {
    return raw.replace(Regex("""^[A-Za-z0-9_]+\s*\["""), "")
        .replace(Regex("""^[A-Za-z0-9_]+\s*\("""), "")
        .replace(Regex("""^[A-Za-z0-9_]+\s*\{"""), "")
        .replace(Regex("""\]$"""), "")
        .replace(Regex("""\)$"""), "")
        .replace(Regex("""\}$"""), "")
        .replace(Regex("""\|.*?\|"""), "") // remove labels like |Yes| or |No|
        .replace("\"", "")
        .replace("'", "")
        .trim()
}

// ---------------------------------------------------------
// INLINE MARKDOWN PARSER (BOLD, ITALIC, HIGHLIGHT, CODE)
// ---------------------------------------------------------

fun parseInlineMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        // Regex patterns
        val highlightRegex = Regex("""==(.*?)==|<mark>(.*?)</mark>""", RegexOption.DOT_MATCHES_ALL)
        val boldRegex = Regex("""\*\*(.*?)\*\*""", RegexOption.DOT_MATCHES_ALL)
        val italicRegex = Regex("""\*(.*?)\*""", RegexOption.DOT_MATCHES_ALL)
        val codeRegex = Regex("""`(.*?)`""", RegexOption.DOT_MATCHES_ALL)

        val tokens = mutableListOf<Token>()

        // 1. Highlight matches (vivid yellow/cyan background)
        highlightRegex.findAll(text).forEach {
            val content = it.groupValues[1].ifEmpty { it.groupValues[2] }
            tokens.add(Token(it.range, "highlight", content))
        }

        // 2. Bold matches
        boldRegex.findAll(text).forEach { match ->
            if (tokens.none { it.range.contains(match.range.first) || it.range.contains(match.range.last) }) {
                tokens.add(Token(match.range, "bold", match.groupValues[1]))
            }
        }

        // 3. Italic matches
        italicRegex.findAll(text).forEach { match ->
            if (tokens.none { it.range.contains(match.range.first) || it.range.contains(match.range.last) }) {
                tokens.add(Token(match.range, "italic", match.groupValues[1]))
            }
        }

        // 4. Code matches
        codeRegex.findAll(text).forEach { match ->
            if (tokens.none { it.range.contains(match.range.first) || it.range.contains(match.range.last) }) {
                tokens.add(Token(match.range, "code", match.groupValues[1]))
            }
        }

        tokens.sortBy { it.range.first }

        var lastIndex = 0
        for (token in tokens) {
            if (token.range.first > lastIndex) {
                append(text.substring(lastIndex, token.range.first))
            }

            val spanStyle = when (token.type) {
                "highlight" -> SpanStyle(
                    background = Color(0xFFFDE047).copy(alpha = 0.35f),
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFF59E0B)
                )
                "bold" -> SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFF00E5FF))
                "italic" -> SpanStyle(fontStyle = FontStyle.Italic)
                "code" -> SpanStyle(
                    fontFamily = FontFamily.Monospace,
                    background = Color.LightGray.copy(alpha = 0.2f),
                    color = Color(0xFFA78BFA)
                )
                else -> SpanStyle()
            }

            withStyle(spanStyle) {
                append(token.content)
            }

            lastIndex = token.range.last + 1
        }

        if (lastIndex < text.length) {
            append(text.substring(lastIndex))
        }
    }
}

data class Token(val range: IntRange, val type: String, val content: String)

// ---------------------------------------------------------
// COMPOSABLES FOR SPECIAL BLOCKS
// ---------------------------------------------------------

/**
 * 1. Eye-catchy Flowchart & Process Diagrams
 */
@Composable
fun FlowDiagramView(diagram: ContentBlock.FlowDiagram) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.AccountTree,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = diagram.title,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Steps rendered in sequence with connected arrows
            diagram.steps.forEachIndexed { index, step ->
                val isLast = index == diagram.steps.size - 1

                val nodeColor = if (step.isDecision) {
                    Color(0xFFF59E0B) // Amber for Decision/Checkpoint
                } else if (index == 0) {
                    Color(0xFF00E5FF) // Cyan for Start
                } else if (isLast) {
                    Color(0xFF10B981) // Emerald for Outcome
                } else {
                    MaterialTheme.colorScheme.primary
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    // Left Column: Step Indicator & Connecting Line
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(32.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = nodeColor,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${index + 1}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.Black
                                )
                            }
                        }
                        if (!isLast) {
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .height(36.dp)
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(nodeColor.copy(alpha = 0.7f), MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                                        )
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Right Card: Step Content
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = if (isLast) 0.dp else 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                        ),
                        border = BorderStroke(1.dp, nodeColor.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = step.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (step.isDecision) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFFF59E0B).copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                "DECISION",
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color(0xFFF59E0B),
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                if (step.description != null) {
                                    Text(
                                        text = step.description,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                            if (!isLast) {
                                Icon(
                                    Icons.Default.ArrowDownward,
                                    contentDescription = null,
                                    tint = nodeColor.copy(alpha = 0.7f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 2. Accommodating Markdown Tables (Horizontal Scrollable)
 */
@Composable
fun MarkdownTableView(table: ContentBlock.Table) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Icon(
                    Icons.Default.TableChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "Data & Classification Table",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    "Swipe horizontally ↔",
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            // Horizontally Scrollable Table Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                Column(
                    modifier = Modifier.width(IntrinsicSize.Max)
                ) {
                    // Header Row
                    Row(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        table.headers.forEach { header ->
                            Box(
                                modifier = Modifier
                                    .widthIn(min = 110.dp, max = 220.dp)
                                    .padding(horizontal = 8.dp)
                            ) {
                                Text(
                                    text = header,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))

                    // Data Rows with alternating zebra shading
                    table.rows.forEachIndexed { rowIndex, rowCells ->
                        val isEven = rowIndex % 2 == 0
                        val rowBg = if (isEven) Color.Transparent else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)

                        Row(
                            modifier = Modifier
                                .background(rowBg)
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            table.headers.indices.forEach { colIndex ->
                                val cellContent = rowCells.getOrNull(colIndex) ?: "-"
                                Box(
                                    modifier = Modifier
                                        .widthIn(min = 110.dp, max = 220.dp)
                                        .padding(horizontal = 8.dp)
                                    ) {
                                    Text(
                                        text = cellContent,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                        if (rowIndex < table.rows.size - 1) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                        }
                    }
                }
            }
        }
    }
}

/**
 * 3. Smart Callout Cards
 */
@Composable
fun CalloutCard(callout: ContentBlock.Callout) {
    val (accentColor, icon) = when (callout.type) {
        CalloutType.WARNING -> Color(0xFFEF4444) to Icons.Default.Warning
        CalloutType.TIP -> Color(0xFF10B981) to Icons.Default.Lightbulb
        CalloutType.CLINICAL -> Color(0xFF00E5FF) to Icons.Default.MedicalServices
        CalloutType.NOTE -> Color(0xFF6366F1) to Icons.Default.Info
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = accentColor.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f))
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = callout.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = accentColor
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = callout.message,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

/**
 * 4. Interactive YouTube Video Recommendation Card
 */
@Composable
fun VideoRecommendationCard(title: String, url: String, onOpen: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() },
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E24)
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFFF0000).copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFFFF0000).copy(alpha = 0.15f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color(0xFFFF3333),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFFFF0000).copy(alpha = 0.2f)
                ) {
                    Text(
                        "YOUTUBE RECOMMENDATION",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFF5555),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color.White
                )
                Text(
                    text = "Tap to launch video lecture",
                    fontSize = 10.sp,
                    color = Color.LightGray
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Color.LightGray,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * 5. Web Reference Card
 */
@Composable
fun WebReferenceCard(title: String, url: String, onOpen: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Language,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = url,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                Icons.Default.OpenInNew,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
