package com.example.edu_ai.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.edu_ai.data.model.HighlightColor
import com.example.edu_ai.data.model.TextHighlight
import com.example.edu_ai.utils.MathSanitizer
import java.util.regex.Pattern

/**
 * High-performance Markdown, Math, Diagram, Table, and Multi-Color Highlighted Text Renderer.
 *
 * Core Features:
 * 1. Automatic LaTeX & Math Symbol Sanitization (removes raw $, \rightarrow, \ge, \le, renders FEV₁, PaO₂, etc.).
 * 2. True Column-Aligned Markdown Tables with synchronized horizontal scrolling, auto-fit widths, and alignments.
 * 3. Flowchart, Metric/Bar Chart, and Clinical Algorithm cards for structured data visualization.
 * 4. Multi-Color Persistent Highlighting (Yellow, Green, Cyan, Purple, Coral).
 * 5. Full Markdown Hierarchy (Headings H1-H4, Bulleted lists, Dividers, Callout cards, Media recommendations).
 */
@Composable
fun FormattedText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    highlights: List<TextHighlight> = emptyList(),
    onLinkClicked: ((String) -> Unit)? = null
) {
    val sanitized = remember(text) { MathSanitizer.sanitize(text) }
    val blocks = remember(sanitized) { parseMarkdownBlocks(sanitized) }
    val context = LocalContext.current

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (block in blocks) {
            when (block) {
                is ContentBlock.Heading -> {
                    HeadingView(block = block, highlights = highlights)
                }
                is ContentBlock.BulletItem -> {
                    BulletItemView(block = block, highlights = highlights, style = style)
                }
                is ContentBlock.Divider -> {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                        thickness = 1.dp
                    )
                }
                is ContentBlock.Table -> {
                    MarkdownTableView(table = block, highlights = highlights)
                }
                is ContentBlock.MetricChart -> {
                    MetricChartView(chart = block)
                }
                is ContentBlock.StepSequence -> {
                    StepSequenceView(sequence = block, highlights = highlights)
                }
                is ContentBlock.ClinicalAlgorithm -> {
                    ClinicalAlgorithmView(algorithm = block, highlights = highlights)
                }
                is ContentBlock.FlowDiagram -> {
                    FlowDiagramView(diagram = block, highlights = highlights)
                }
                is ContentBlock.AsciiDiagram -> {
                    AsciiDiagramCard(diagram = block)
                }
                is ContentBlock.Callout -> {
                    CalloutCard(callout = block, highlights = highlights)
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
                is ContentBlock.CodeBlock -> {
                    CodeBlockCard(block = block)
                }
                is ContentBlock.Paragraph -> {
                    val annotated = remember(block.text, highlights) {
                        parseInlineMarkdown(block.text, highlights)
                    }
                    SelectionContainer {
                        Text(
                            text = annotated,
                            style = style.copy(lineHeight = 22.sp),
                            color = MaterialTheme.colorScheme.onSurface,
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
    data class Heading(val level: Int, val text: String) : ContentBlock()
    data class BulletItem(val text: String, val level: Int = 0) : ContentBlock()
    object Divider : ContentBlock()
    data class Table(
        val headers: List<String>,
        val rows: List<List<String>>,
        val alignments: List<TextAlign> = emptyList()
    ) : ContentBlock()
    data class MetricChart(val title: String, val items: List<MetricItem>) : ContentBlock()
    data class StepSequence(val title: String, val steps: List<SequenceStep>) : ContentBlock()
    data class ClinicalAlgorithm(val title: String, val steps: List<AlgorithmStep>) : ContentBlock()
    data class FlowDiagram(val title: String, val steps: List<FlowStep>) : ContentBlock()
    data class AsciiDiagram(val title: String, val rawContent: String) : ContentBlock()
    data class Callout(val type: CalloutType, val title: String, val message: String) : ContentBlock()
    data class YouTubeVideo(val title: String, val url: String) : ContentBlock()
    data class WebReference(val title: String, val url: String) : ContentBlock()
    data class CodeBlock(val code: String, val language: String = "") : ContentBlock()
    data class Paragraph(val text: String) : ContentBlock()
}

data class MetricItem(
    val label: String,
    val value: Float,
    val unit: String = "%",
    val description: String? = null
)

data class SequenceStep(
    val stepNumber: String,
    val title: String,
    val detail: String? = null,
    val isDecision: Boolean = false
)

data class FlowStep(
    val id: String,
    val title: String,
    val description: String? = null,
    val isDecision: Boolean = false,
    val tag: String? = null
)

data class AlgorithmStep(
    val stepTitle: String,
    val subtitle: String? = null,
    val branches: List<AlgorithmBranch> = emptyList(),
    val notes: List<String> = emptyList()
)

data class AlgorithmBranch(
    val condition: String,
    val outcome: String,
    val nextAction: String? = null,
    val isCritical: Boolean = false
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
        val trimmed = line.trim()

        if (trimmed.isEmpty()) {
            i++
            continue
        }

        // 1. Horizontal Divider: --- or ___ or ***
        if (trimmed == "---" || trimmed == "___" || trimmed == "***" || trimmed.matches(Regex("""^[-*_]{3,}$"""))) {
            blocks.add(ContentBlock.Divider)
            i++
            continue
        }

        // 2. Headings: #, ##, ###, ####
        if (trimmed.startsWith("#")) {
            val hashCount = trimmed.takeWhile { it == '#' }.length
            if (hashCount in 1..6 && trimmed.length > hashCount && trimmed[hashCount] == ' ') {
                val headingText = trimmed.substring(hashCount).trim()
                blocks.add(ContentBlock.Heading(level = hashCount, text = headingText))
                i++
                continue
            }
        }

        // 3. Code Block / Diagrams (```)
        if (trimmed.startsWith("```")) {
            val lang = trimmed.removePrefix("```").trim().lowercase()
            val codeLines = mutableListOf<String>()
            i++
            while (i < lines.size && !lines[i].trim().startsWith("```")) {
                codeLines.add(lines[i])
                i++
            }
            if (i < lines.size) i++ // skip closing ```

            val joined = codeLines.joinToString("\n")

            // Check if this code block represents an ASCII diagram / flowchart / metric chart
            if (isAsciiOrFlowDiagram(joined, lang)) {
                val diagram = parseClinicalDiagramOrTree(codeLines)
                blocks.add(diagram)
            } else {
                blocks.add(ContentBlock.CodeBlock(code = joined, language = lang))
            }
            continue
        }

        // 4. Markdown Table: supports lines with pipes or starting/ending without outer pipes
        if (isTableCandidate(lines, i)) {
            val tableLines = mutableListOf<String>()
            while (i < lines.size && isTableRowLine(lines[i])) {
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

        // 5. Plaintext Metric Chart (e.g. Mild: [====] 80% or FEV1: 75%)
        val metricChart = tryParseMetricChart(lines, i)
        if (metricChart != null) {
            blocks.add(metricChart.first)
            i += metricChart.second
            continue
        }

        // 6. Plaintext Step Sequence (e.g. Step 1: ..., Step 2: ... or 1. ... -> 2. ...)
        val stepSeq = tryParseStepSequence(lines, i)
        if (stepSeq != null) {
            blocks.add(stepSeq.first)
            i += stepSeq.second
            continue
        }

        // 7. Plaintext Tree or Step Flowchart without code fences
        if (trimmed.startsWith("[Systematic Flow]") || trimmed.contains("├──") || trimmed.contains("└──") || trimmed.contains("┌─")) {
            val diagLines = mutableListOf<String>()
            while (i < lines.size) {
                val cur = lines[i]
                val curTrim = cur.trim()
                if (curTrim.isEmpty() && diagLines.size > 2) {
                    if (i + 1 < lines.size && (lines[i + 1].contains("├──") || lines[i + 1].contains("└──") || lines[i + 1].contains("[ STEP"))) {
                        diagLines.add("")
                        i++
                        continue
                    } else break
                }
                if (curTrim.startsWith("#") || curTrim.startsWith("---") || curTrim.startsWith("```")) {
                    break
                }
                diagLines.add(cur)
                i++
            }
            if (diagLines.isNotEmpty()) {
                blocks.add(parseClinicalDiagramOrTree(diagLines))
                continue
            }
        }

        // 8. Callouts (> [!NOTE], > [!TIP], > [!WARNING], > [!CLINICAL])
        if (trimmed.startsWith("> [!")) {
            val typeStr = trimmed.substringAfter("> [!").substringBefore("]").uppercase()
            val calloutType = when (typeStr) {
                "WARNING", "CAUTION", "TRAP", "ALERT" -> CalloutType.WARNING
                "TIP", "SUCCESS", "PEARL" -> CalloutType.TIP
                "CLINICAL", "MED", "OSCE" -> CalloutType.CLINICAL
                else -> CalloutType.NOTE
            }
            val title = trimmed.substringAfter("]", "").trim().ifBlank { typeStr }
            val messageLines = mutableListOf<String>()
            i++
            while (i < lines.size && lines[i].trim().startsWith(">")) {
                messageLines.add(lines[i].trim().removePrefix(">").trim())
                i++
            }
            blocks.add(ContentBlock.Callout(calloutType, title, messageLines.joinToString("\n")))
            continue
        }

        // 9. Bullet items (* item or - item or + item)
        val bulletMatch = Regex("""^(\s*)([*+-])\s+(.*)$""").find(line)
        if (bulletMatch != null) {
            val indent = bulletMatch.groupValues[1].length / 2
            val bulletText = bulletMatch.groupValues[3].trim()
            blocks.add(ContentBlock.BulletItem(text = bulletText, level = indent.coerceIn(0, 3)))
            i++
            continue
        }

        // 10. YouTube URL or [Video Title](youtube_url)
        val ytPattern = "(https?://(?:www\\.)?youtube\\.com/watch\\?v=[\\w-]+|https?://youtu\\.be/[\\w-]+)"
        val ytMatcher = Pattern.compile(ytPattern).matcher(trimmed)
        if (ytMatcher.find()) {
            val url = ytMatcher.group(0) ?: ""
            if (url.isNotEmpty()) {
                val title = if (trimmed.contains("[") && trimmed.contains("]($url)")) {
                    trimmed.substringAfter("[").substringBefore("]")
                } else {
                    "Recommended Video Lecture"
                }
                blocks.add(ContentBlock.YouTubeVideo(title = title, url = url))
                i++
                continue
            }
        }

        // 11. Web reference
        if (trimmed.startsWith("[Reference]") || trimmed.startsWith("[Web]") || (trimmed.startsWith("http") && !trimmed.contains("youtube"))) {
            val url = if (trimmed.startsWith("http")) trimmed else trimmed.substringAfter("(").substringBefore(")")
            val title = if (trimmed.contains("[") && trimmed.contains("]")) trimmed.substringAfter("[").substringBefore("]") else "Reference Resource"
            blocks.add(ContentBlock.WebReference(title = title, url = url))
            i++
            continue
        }

        // 12. Regular Paragraph
        val paraLines = mutableListOf<String>()
        while (i < lines.size) {
            val cur = lines[i]
            val curTrim = cur.trim()
            if (curTrim.isEmpty() ||
                curTrim.startsWith("#") ||
                curTrim == "---" ||
                curTrim.startsWith("```") ||
                isTableCandidate(lines, i) ||
                curTrim.startsWith("> [!") ||
                Regex("""^(\s*)([*+-])\s+""").containsMatchIn(cur) ||
                curTrim.startsWith("[Systematic Flow]") ||
                curTrim.contains("├──") ||
                curTrim.contains("└──") ||
                Pattern.compile(ytPattern).matcher(curTrim).find()
            ) {
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

// Helper to identify table candidates
private fun isTableCandidate(lines: List<String>, index: Int): Boolean {
    if (index + 1 >= lines.size) return false
    val header = lines[index].trim()
    val separator = lines[index + 1].trim()
    if (!header.contains("|")) return false
    // Separator line should contain dashes and pipes e.g. |---|---| or ---|--- or |:---|:---:|
    return separator.matches(Regex("""^\|?[\s:-]+(?:\|[\s:-]+)+\|?$"""))
}

private fun isTableRowLine(line: String): Boolean {
    val trimmed = line.trim()
    return trimmed.isNotEmpty() && trimmed.contains("|")
}

private fun isAsciiOrFlowDiagram(text: String, lang: String): Boolean {
    if (lang.contains("mermaid") || lang.contains("flowchart") || lang.contains("graph") || lang.contains("chart")) return true
    if (text.contains("┌") || text.contains("└") || text.contains("├──") || text.contains("└──")) return true
    if (text.contains("[ STEP") || text.contains("[Step") || text.contains("[Systematic Flow]")) return true
    if (text.contains("->") || text.contains("-->") || text.contains("==>")) return true
    if (text.lines().any { it.contains("[=") || it.contains("==") } && text.contains("%")) return true
    return false
}

// ---------------------------------------------------------
// TABLE PARSER (ROBUST WITH COLUMN NORMALIZATION)
// ---------------------------------------------------------

fun parseMarkdownTable(lines: List<String>): ContentBlock.Table? {
    if (lines.size < 2) return null

    fun splitCells(line: String): List<String> {
        var clean = line.trim()
        if (clean.startsWith("|")) clean = clean.substring(1)
        if (clean.endsWith("|")) clean = clean.substring(0, clean.length - 1)
        return clean.split("|").map { it.trim() }
    }

    val rawHeaders = splitCells(lines[0])
    if (rawHeaders.isEmpty()) return null
    val colCount = rawHeaders.size

    val separatorLine = lines[1].trim()
    val rawSep = splitCells(separatorLine)
    val alignments = (0 until colCount).map { idx ->
        val s = rawSep.getOrNull(idx) ?: "---"
        when {
            s.startsWith(":") && s.endsWith(":") -> TextAlign.Center
            s.endsWith(":") -> TextAlign.End
            else -> TextAlign.Start
        }
    }

    val rows = mutableListOf<List<String>>()
    for (k in 2 until lines.size) {
        val line = lines[k].trim()
        if (line.isEmpty()) continue
        // Skip duplicate separator lines
        if (line.matches(Regex("""^\|?[\s:-]+(?:\|[\s:-]+)+\|?$"""))) continue

        val cells = splitCells(line)
        val normalized = when {
            cells.size == colCount -> cells
            cells.size < colCount -> cells + List(colCount - cells.size) { "-" }
            else -> cells.take(colCount)
        }
        rows.add(normalized)
    }

    return if (rawHeaders.isNotEmpty() && rows.isNotEmpty()) {
        ContentBlock.Table(headers = rawHeaders, rows = rows, alignments = alignments)
    } else null
}

// ---------------------------------------------------------
// METRIC & SEQUENCE PARSERS
// ---------------------------------------------------------

private fun tryParseMetricChart(lines: List<String>, startIndex: Int): Pair<ContentBlock.MetricChart, Int>? {
    val items = mutableListOf<MetricItem>()
    var count = 0
    var title = "Clinical Metric & Value Distribution"

    var idx = startIndex
    val firstLine = lines[idx].trim()
    if (firstLine.endsWith(":") && !firstLine.contains("%")) {
        title = firstLine.removeSuffix(":").trim()
        idx++
        count++
    }

    while (idx < lines.size) {
        val l = lines[idx].trim()
        if (l.isEmpty()) break
        // Match "Label: [====] 80%" or "Label: 80%"
        val match = Regex("""^([A-Za-z0-9\s()_%/–-]+?)\s*[:|]\s*(?:\[[=\-#* ]+\])?\s*(\d+(?:\.\d+)?)\s*%(?:\s*-\s*(.+))?$""").find(l)
        if (match != null) {
            val label = match.groupValues[1].trim()
            val value = match.groupValues[2].toFloatOrNull() ?: 0f
            val note = match.groupValues[3].ifBlank { null }
            items.add(MetricItem(label = label, value = value, description = note))
            count++
            idx++
        } else break
    }

    return if (items.size >= 2) {
        Pair(ContentBlock.MetricChart(title = title, items = items), count)
    } else null
}

private fun tryParseStepSequence(lines: List<String>, startIndex: Int): Pair<ContentBlock.StepSequence, Int>? {
    val steps = mutableListOf<SequenceStep>()
    var count = 0
    var title = "Systematic Step Protocol"

    var idx = startIndex
    val firstLine = lines[idx].trim()
    if ((firstLine.endsWith(":") || firstLine.startsWith("#")) && !firstLine.startsWith("Step") && !firstLine.startsWith("1.")) {
        title = firstLine.replace("#", "").removeSuffix(":").trim()
        idx++
        count++
    }

    while (idx < lines.size) {
        val l = lines[idx].trim()
        if (l.isEmpty()) break
        // Match "Step 1: Description" or "1. Description"
        val match = Regex("""^(?:\[?\s*(?:STEP|Step)\s*(\d+)[\s:]*\]?|\b(\d+)\.\s+)\s*(.+)$""").find(l)
        if (match != null) {
            val num = match.groupValues[1].ifEmpty { match.groupValues[2] }
            val fullDesc = match.groupValues[3].trim()
            val stepTitle = if (fullDesc.contains(" - ")) fullDesc.substringBefore(" - ") else fullDesc.substringBefore(":")
            val detail = if (fullDesc.contains(" - ")) fullDesc.substringAfter(" - ") else if (fullDesc.contains(":")) fullDesc.substringAfter(":") else null
            steps.add(SequenceStep(stepNumber = num, title = stepTitle.trim(), detail = detail?.trim()))
            count++
            idx++
        } else break
    }

    return if (steps.size >= 2) {
        Pair(ContentBlock.StepSequence(title = title, steps = steps), count)
    } else null
}

// ---------------------------------------------------------
// CLINICAL DIAGRAM / FLOWCHART PARSER
// ---------------------------------------------------------

fun parseClinicalDiagramOrTree(lines: List<String>): ContentBlock {
    val joined = lines.joinToString("\n")

    // 1. Metric / Bar Chart
    val metricItems = mutableListOf<MetricItem>()
    for (l in lines) {
        val m = Regex("""^([A-Za-z0-9\s()_%/–-]+?)\s*[:|]\s*(?:\[[=\-#* ]+\])?\s*(\d+(?:\.\d+)?)\s*%(?:\s*-\s*(.+))?$""").find(l.trim())
        if (m != null) {
            val label = m.groupValues[1].trim()
            val valFloat = m.groupValues[2].toFloatOrNull() ?: 0f
            metricItems.add(MetricItem(label = label, value = valFloat, description = m.groupValues[3].ifBlank { null }))
        }
    }
    if (metricItems.size >= 2) {
        return ContentBlock.MetricChart(title = "Clinical Severity & Value Grading", items = metricItems)
    }

    // 2. Mermaid flowchart parsing
    if (joined.contains("mermaid") || joined.contains("graph ") || joined.contains("flowchart ") || lines.any { it.contains("-->") || it.contains("->") }) {
        val flowSteps = mutableListOf<FlowStep>()
        val nodeLabels = mutableMapOf<String, String>()
        val edges = mutableListOf<Triple<String, String, String?>>()

        for (l in lines) {
            val trimmed = l.trim()
            if (trimmed.startsWith("graph") || trimmed.startsWith("flowchart") || trimmed.startsWith("```")) continue

            val regex = Regex("""([A-Za-z0-9_]+)(?:\[(.*?)\]|\{(.*?)\})?\s*-->?(?:\|(.*?)\|)?\s*([A-Za-z0-9_]+)(?:\[(.*?)\]|\{(.*?)\})?""")
            regex.findAll(trimmed).forEach { match ->
                val fromId = match.groupValues[1]
                val fromLabel = match.groupValues[2].ifEmpty { match.groupValues[3] }
                val condition = match.groupValues[4].ifEmpty { null }
                val toId = match.groupValues[5]
                val toLabel = match.groupValues[6].ifEmpty { match.groupValues[7] }

                if (fromLabel.isNotEmpty()) nodeLabels[fromId] = fromLabel
                else nodeLabels.putIfAbsent(fromId, fromId)

                if (toLabel.isNotEmpty()) nodeLabels[toId] = toLabel
                else nodeLabels.putIfAbsent(toId, toId)

                edges.add(Triple(fromId, toId, condition))
            }
        }

        if (edges.isNotEmpty()) {
            val visited = mutableSetOf<String>()
            var stepNum = 1
            for ((from, to, cond) in edges) {
                if (!visited.contains(from)) {
                    val label = nodeLabels[from] ?: from
                    flowSteps.add(FlowStep(id = from, title = "Step $stepNum: $label", tag = "Initial Decision"))
                    visited.add(from)
                    stepNum++
                }
                val toLabel = nodeLabels[to] ?: to
                val tagStr = if (cond != null) "Condition: $cond" else null
                flowSteps.add(FlowStep(id = to, title = "Step $stepNum: $toLabel", tag = tagStr, isDecision = cond != null))
                visited.add(to)
                stepNum++
            }
            if (flowSteps.isNotEmpty()) {
                return ContentBlock.FlowDiagram(title = "Clinical Decision & Diagnostic Flow", steps = flowSteps)
            }
        }
    }

    // 3. Tree diagram with ├── or └──
    if (joined.contains("├──") || joined.contains("└──")) {
        var title = "Systematic Clinical Flow Protocol"
        val steps = mutableListOf<FlowStep>()

        for (line in lines) {
            val t = line.trim()
            if (t.startsWith("[") && t.endsWith("]") && !t.contains("├──") && !t.contains("└──")) {
                title = t.removePrefix("[").removeSuffix("]").trim()
                continue
            }
            if (t.contains("├──") || t.contains("└──")) {
                val clean = t.replace(Regex("""^[│\s]*[├└]──+\s*"""), "").trim()
                if (clean.isNotEmpty()) {
                    val isBranch = clean.contains(":") || clean.contains("->")
                    steps.add(FlowStep(id = "step_${steps.size + 1}", title = clean, isDecision = isBranch))
                }
            }
        }

        if (steps.isNotEmpty()) {
            return ContentBlock.FlowDiagram(title = title, steps = steps)
        }
    }

    // 4. Diagnostic Algorithm (e.g. [ STEP 1... ], [ STEP 2... ])
    if (joined.contains("[ STEP") || joined.contains("[Step") || joined.contains("OBSTRUCTIVE DEFECT") || joined.contains("RESTRICTIVE DEFECT")) {
        val algorithmSteps = mutableListOf<AlgorithmStep>()
        var currentTitle = "Spirometry & Clinical Protocol"
        var currentStepTitle = ""
        val currentBranches = mutableListOf<AlgorithmBranch>()
        val currentNotes = mutableListOf<String>()

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isEmpty() || line == "|" || line.contains("┌─") || line.contains("─┐") || line.contains("▼")) continue

            if (line.startsWith("[ STEP") || line.startsWith("[Step")) {
                if (currentStepTitle.isNotEmpty()) {
                    algorithmSteps.add(AlgorithmStep(currentStepTitle, branches = currentBranches.toList(), notes = currentNotes.toList()))
                    currentBranches.clear()
                    currentNotes.clear()
                }
                currentStepTitle = line.removePrefix("[").removeSuffix("]").trim()
                continue
            }

            if (line.startsWith("[") && line.contains("]") && (line.contains("<") || line.contains(">") || line.contains("70%") || line.contains("80%"))) {
                val condition = line.removePrefix("[").substringBefore("]").trim()
                val remaining = line.substringAfter("]", "").trim()
                val isDefect = remaining.contains("DEFECT") || remaining.contains("OBSTRUCTIVE") || remaining.contains("RESTRICTIVE")
                currentBranches.add(AlgorithmBranch(condition = condition, outcome = remaining.ifEmpty { "Diagnostic Path" }, isCritical = isDefect))
                continue
            }

            if (line.contains("OBSTRUCTIVE DEFECT") || line.contains("RESTRICTIVE DEFECT") || line.contains("NORMAL")) {
                if (currentBranches.isNotEmpty()) {
                    val last = currentBranches.removeAt(currentBranches.size - 1)
                    currentBranches.add(last.copy(outcome = line, isCritical = line.contains("DEFECT")))
                } else {
                    currentBranches.add(AlgorithmBranch(condition = "Finding", outcome = line, isCritical = line.contains("DEFECT")))
                }
                continue
            }

            if (line.startsWith("•") || line.startsWith("-") || line.startsWith("*")) {
                currentNotes.add(line.removePrefix("•").removePrefix("-").removePrefix("*").trim())
                continue
            }
        }

        if (currentStepTitle.isNotEmpty()) {
            algorithmSteps.add(AlgorithmStep(currentStepTitle, branches = currentBranches.toList(), notes = currentNotes.toList()))
        }

        if (algorithmSteps.isNotEmpty()) {
            return ContentBlock.ClinicalAlgorithm(title = currentTitle, steps = algorithmSteps)
        }
    }

    // Default fallback to clean monospaced ASCII diagram card with horizontal scrolling
    return ContentBlock.AsciiDiagram(title = "Clinical Flow & Reference Diagram", rawContent = joined)
}

// ---------------------------------------------------------
// INLINE MARKDOWN & MULTI-COLOR HIGHLIGHT PARSER
// ---------------------------------------------------------

fun parseInlineMarkdown(text: String, highlights: List<TextHighlight> = emptyList()): AnnotatedString {
    return buildAnnotatedString {
        val tokens = mutableListOf<TokenSpan>()

        // 1. User persistent multi-color highlights
        for (hl in highlights) {
            val hlText = hl.text.trim()
            if (hlText.isNotEmpty()) {
                var searchIdx = 0
                while (searchIdx < text.length) {
                    val found = text.indexOf(hlText, searchIdx, ignoreCase = true)
                    if (found == -1) break
                    val range = found until (found + hlText.length)
                    tokens.add(TokenSpan(range, "user_highlight", text.substring(range), extra = hl.colorHex))
                    searchIdx = found + hlText.length
                }
            }
        }

        // 2. Markdown Highlight ==text== or <mark>text</mark>
        val highlightRegex = Regex("""==(.*?)==|<mark>(.*?)</mark>""", RegexOption.DOT_MATCHES_ALL)
        highlightRegex.findAll(text).forEach {
            val content = it.groupValues[1].ifEmpty { it.groupValues[2] }
            tokens.add(TokenSpan(it.range, "highlight", content))
        }

        // 3. Bold matches **text**
        val boldRegex = Regex("""\*\*(.*?)\*\*""", RegexOption.DOT_MATCHES_ALL)
        boldRegex.findAll(text).forEach { match ->
            if (tokens.none { it.range.first <= match.range.first && it.range.last >= match.range.last }) {
                tokens.add(TokenSpan(match.range, "bold", match.groupValues[1]))
            }
        }

        // 4. Italic matches *text*
        val italicRegex = Regex("""(?<!\*)\*([^*]+)\*(?!\*)""", RegexOption.DOT_MATCHES_ALL)
        italicRegex.findAll(text).forEach { match ->
            if (tokens.none { it.range.first <= match.range.first && it.range.last >= match.range.last }) {
                tokens.add(TokenSpan(match.range, "italic", match.groupValues[1]))
            }
        }

        // 5. Code matches `text`
        val codeRegex = Regex("""`([^`]+)`""", RegexOption.DOT_MATCHES_ALL)
        codeRegex.findAll(text).forEach { match ->
            if (tokens.none { it.range.first <= match.range.first && it.range.last >= match.range.last }) {
                tokens.add(TokenSpan(match.range, "code", match.groupValues[1]))
            }
        }

        tokens.sortBy { it.range.first }

        // Remove overlapping tokens by preferring user_highlight and bold
        val nonOverlapping = mutableListOf<TokenSpan>()
        for (token in tokens) {
            val overlap = nonOverlapping.any {
                (token.range.first >= it.range.first && token.range.first <= it.range.last) ||
                (token.range.last >= it.range.first && token.range.last <= it.range.last)
            }
            if (!overlap) {
                nonOverlapping.add(token)
            }
        }

        var lastIndex = 0
        for (token in nonOverlapping) {
            if (token.range.first > lastIndex) {
                append(text.substring(lastIndex, token.range.first))
            }

            val spanStyle = when (token.type) {
                "user_highlight" -> {
                    val colorHex = token.extra ?: "#FEF08A"
                    val parsedColor = try {
                        Color(android.graphics.Color.parseColor(colorHex))
                    } catch (e: Exception) {
                        Color(0xFFFEF08A)
                    }
                    SpanStyle(
                        background = parsedColor.copy(alpha = 0.38f),
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                "highlight" -> SpanStyle(
                    background = Color(0xFFFDE047).copy(alpha = 0.35f),
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFF59E0B)
                )
                "bold" -> SpanStyle(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF38BDF8) // Crisp Sky Cyan
                )
                "italic" -> SpanStyle(fontStyle = FontStyle.Italic)
                "code" -> SpanStyle(
                    fontFamily = FontFamily.Monospace,
                    background = Color(0xFF1E293B),
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

data class TokenSpan(val range: IntRange, val type: String, val content: String, val extra: String? = null)

// ---------------------------------------------------------
// COMPOSABLES: HEADINGS & BULLETS
// ---------------------------------------------------------

@Composable
fun HeadingView(block: ContentBlock.Heading, highlights: List<TextHighlight>) {
    val annotated = remember(block.text, highlights) {
        parseInlineMarkdown(block.text, highlights)
    }

    when (block.level) {
        1 -> {
            Text(
                text = annotated,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
            )
        }
        2 -> {
            Column(modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(4.dp, 20.dp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = annotated,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
            }
        }
        3 -> {
            Text(
                text = annotated,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF38BDF8),
                modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
            )
        }
        else -> {
            Text(
                text = annotated,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
            )
        }
    }
}

@Composable
fun BulletItemView(
    block: ContentBlock.BulletItem,
    highlights: List<TextHighlight>,
    style: TextStyle
) {
    val annotated = remember(block.text, highlights) {
        parseInlineMarkdown(block.text, highlights)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (block.level * 16).dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
            modifier = Modifier
                .padding(top = 8.dp)
                .size(6.dp)
        ) {}
        Spacer(modifier = Modifier.width(10.dp))
        SelectionContainer {
            Text(
                text = annotated,
                style = style.copy(lineHeight = 22.sp),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// ---------------------------------------------------------
// COMPOSABLES: TABLES (TRUE COLUMN ALIGNMENT & RESPONSIVE FIT)
// ---------------------------------------------------------

@Composable
fun MarkdownTableView(
    table: ContentBlock.Table,
    highlights: List<TextHighlight> = emptyList()
) {
    if (table.headers.isEmpty()) return

    val columnCount = table.headers.size

    // Calculate base minimum widths based on content length
    val baseColWidths = remember(table) {
        (0 until columnCount).map { colIdx ->
            val headerLen = table.headers.getOrNull(colIdx)?.length ?: 0
            val maxRowLen = table.rows.maxOfOrNull { row ->
                row.getOrNull(colIdx)?.length ?: 0
            } ?: 0
            val maxLen = maxOf(headerLen, maxRowLen)

            when {
                maxLen > 35 -> 200.dp
                maxLen > 20 -> 150.dp
                maxLen > 10 -> 120.dp
                else -> 95.dp
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            // Header Info Bar
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
                    "Clinical Data & Classification Table",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    "${columnCount} columns",
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val availableWidth = maxWidth - 16.dp
                val totalMinWidth = baseColWidths.fold(0.dp) { acc, d -> acc + d }
                val requiresScroll = totalMinWidth > availableWidth

                // If fits on screen, distribute available width evenly/proportionally
                val computedColWidths = if (!requiresScroll && columnCount > 0) {
                    val share = availableWidth / columnCount
                    List(columnCount) { maxOf(share, baseColWidths[it]) }
                } else {
                    baseColWidths
                }

                val scrollModifier = if (requiresScroll) {
                    Modifier.horizontalScroll(rememberScrollState())
                } else {
                    Modifier
                }

                Box(modifier = Modifier.fillMaxWidth().then(scrollModifier)) {
                    Column(
                        modifier = if (requiresScroll) Modifier.width(IntrinsicSize.Max) else Modifier.fillMaxWidth()
                    ) {
                        // Header Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                    shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                                )
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            table.headers.forEachIndexed { colIndex, header ->
                                val colWidth = computedColWidths.getOrElse(colIndex) { 120.dp }
                                val align = table.alignments.getOrElse(colIndex) { TextAlign.Start }
                                Box(
                                    modifier = Modifier
                                        .width(colWidth)
                                        .padding(horizontal = 8.dp)
                                ) {
                                    val cleanHeader = MathSanitizer.sanitize(header)
                                    Text(
                                        text = parseInlineMarkdown(cleanHeader, highlights),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        textAlign = align,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))

                        // Data Rows
                        table.rows.forEachIndexed { rowIndex, rowCells ->
                            val isEven = rowIndex % 2 == 0
                            val rowBg = if (isEven) Color.Transparent else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(rowBg)
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                (0 until columnCount).forEach { colIndex ->
                                    val colWidth = computedColWidths.getOrElse(colIndex) { 120.dp }
                                    val align = table.alignments.getOrElse(colIndex) { TextAlign.Start }
                                    val cellRaw = rowCells.getOrNull(colIndex) ?: "-"
                                    val cleanCell = MathSanitizer.sanitize(cellRaw)

                                    Box(
                                        modifier = Modifier
                                            .width(colWidth)
                                            .padding(horizontal = 8.dp)
                                    ) {
                                        SelectionContainer {
                                            Text(
                                                text = parseInlineMarkdown(cleanCell, highlights),
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                lineHeight = 16.sp,
                                                textAlign = align,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
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
}

// ---------------------------------------------------------
// COMPOSABLES: METRIC & BAR CHARTS
// ---------------------------------------------------------

@Composable
fun MetricChartView(chart: ContentBlock.MetricChart) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF00E5FF).copy(alpha = 0.18f),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.BarChart,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        "CLINICAL METRIC GRADING",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF00E5FF)
                    )
                    Text(
                        text = chart.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            chart.items.forEachIndexed { idx, item ->
                val progress = (item.value / 100f).coerceIn(0f, 1f)
                val barColor = when {
                    item.value >= 80f -> Color(0xFF10B981) // Green / Normal
                    item.value >= 50f -> Color(0xFFF59E0B) // Amber / Moderate
                    item.value >= 30f -> Color(0xFFF97316) // Orange / Severe
                    else -> Color(0xFFEF4444) // Red / Critical
                }

                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = barColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "${item.value.toInt()}${item.unit}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = barColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Progress bar track
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(barColor.copy(alpha = 0.7f), barColor)
                                    )
                                )
                        )
                    }

                    if (item.description != null) {
                        Text(
                            text = item.description,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------
// COMPOSABLES: STEP SEQUENCES
// ---------------------------------------------------------

@Composable
fun StepSequenceView(sequence: ContentBlock.StepSequence, highlights: List<TextHighlight>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.AltRoute,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        "SYSTEMATIC CLINICAL PROTOCOL",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = sequence.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            sequence.steps.forEachIndexed { index, step ->
                val isLast = index == sequence.steps.size - 1

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(28.dp)) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    step.stepNumber,
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
                                    .height(32.dp)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = if (isLast) 0.dp else 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = parseInlineMarkdown(MathSanitizer.sanitize(step.title), highlights),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (step.detail != null) {
                                Text(
                                    text = parseInlineMarkdown(MathSanitizer.sanitize(step.detail), highlights),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------
// COMPOSABLES: CLINICAL ALGORITHM & FLOWCHARTS
// ---------------------------------------------------------

@Composable
fun ClinicalAlgorithmView(
    algorithm: ContentBlock.ClinicalAlgorithm,
    highlights: List<TextHighlight>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
        border = BorderStroke(1.5.dp, Color(0xFF0284C7).copy(alpha = 0.4f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF0284C7).copy(alpha = 0.2f),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.AccountTree,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFF0284C7).copy(alpha = 0.2f)) {
                        Text(
                            "DIAGNOSTIC ALGORITHM",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF38BDF8),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = algorithm.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Steps
            algorithm.steps.forEachIndexed { stepIdx, step ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.primary) {
                                Text(
                                    "STEP ${stepIdx + 1}",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.Black,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = MathSanitizer.sanitize(step.stepTitle),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (!step.branches.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            step.branches.forEach { branch ->
                                val branchColor = if (branch.isCritical) Color(0xFFEF4444) else Color(0xFF10B981)
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                    colors = CardDefaults.cardColors(containerColor = branchColor.copy(alpha = 0.08f)),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, branchColor.copy(alpha = 0.3f))
                                ) {
                                    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Surface(shape = RoundedCornerShape(4.dp), color = branchColor.copy(alpha = 0.2f)) {
                                            Text(
                                                MathSanitizer.sanitize(branch.condition),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = branchColor,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = MathSanitizer.sanitize(branch.outcome),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (branch.nextAction != null) {
                                                Text(
                                                    text = MathSanitizer.sanitize(branch.nextAction),
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.outline
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (!step.notes.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            step.notes.forEach { note ->
                                Row(modifier = Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.Top) {
                                    Text("•", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = parseInlineMarkdown(MathSanitizer.sanitize(note), highlights),
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
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

@Composable
fun FlowDiagramView(
    diagram: ContentBlock.FlowDiagram,
    highlights: List<TextHighlight> = emptyList()
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
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

            diagram.steps.forEachIndexed { index, step ->
                val isLast = index == diagram.steps.size - 1
                val nodeColor = if (step.isDecision) Color(0xFFF59E0B)
                else if (index == 0) Color(0xFF00E5FF)
                else if (isLast) Color(0xFF10B981)
                else MaterialTheme.colorScheme.primary

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(32.dp)) {
                        Surface(shape = CircleShape, color = nodeColor, modifier = Modifier.size(24.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("${index + 1}", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.Black)
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

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = if (isLast) 0.dp else 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
                        border = BorderStroke(1.dp, nodeColor.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            if (step.tag != null) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = nodeColor.copy(alpha = 0.15f),
                                    modifier = Modifier.padding(bottom = 4.dp)
                                ) {
                                    Text(
                                        text = step.tag,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = nodeColor,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = parseInlineMarkdown(MathSanitizer.sanitize(step.title), highlights),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (step.description != null) {
                                Text(
                                    text = parseInlineMarkdown(MathSanitizer.sanitize(step.description), highlights),
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AsciiDiagramCard(diagram: ContentBlock.AsciiDiagram) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Schema,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = diagram.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = Color(0xFF38BDF8)
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "Swipe horizontally ↔",
                    fontSize = 9.sp,
                    color = Color.Gray
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .background(Color(0xFF020617), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Text(
                    text = diagram.rawContent,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    color = Color(0xFFE2E8F0),
                    softWrap = false
                )
            }
        }
    }
}

@Composable
fun CodeBlockCard(block: ContentBlock.CodeBlock) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(12.dp)
        ) {
            Text(
                text = block.code,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = Color(0xFFE2E8F0),
                softWrap = false
            )
        }
    }
}

// ---------------------------------------------------------
// COMPOSABLES: CALLOUTS & MEDIA
// ---------------------------------------------------------

@Composable
fun CalloutCard(callout: ContentBlock.Callout, highlights: List<TextHighlight>) {
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
                SelectionContainer {
                    Text(
                        text = parseInlineMarkdown(MathSanitizer.sanitize(callout.message), highlights),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

fun extractYouTubeVideoId(url: String): String? {
    val pattern = "(?:youtube\\.com/(?:watch\\?v=|embed/)|youtu\\.be/)([\\w-]{11})"
    val matcher = java.util.regex.Pattern.compile(pattern).matcher(url)
    return if (matcher.find()) matcher.group(1) else null
}

@Composable
fun VideoRecommendationCard(title: String, url: String, onOpen: () -> Unit) {
    val videoId = remember(url) { extractYouTubeVideoId(url) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF18181E)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFE50914).copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Simulated 16:9 Video Canvas / Stage
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF3B0B0B),
                                Color(0xFF1A141A),
                                Color(0xFF0F0E14)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Background subtle grid/lines
                Column(
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFE50914).copy(alpha = 0.85f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(Color.White, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "YOUTUBE LECTURE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                        }

                        if (videoId != null) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color.Black.copy(alpha = 0.7f),
                                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f))
                            ) {
                                Text(
                                    "ID: $videoId",
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.LightGray,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color.Black.copy(alpha = 0.75f)
                        ) {
                            Text(
                                "HIGH-YIELD",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFC107),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Centered Glowing Play Action
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFE50914),
                    shadowElevation = 8.dp,
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Play Video",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            // Bottom details and action bar
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = title.ifBlank { "Recommended Video Lecture" },
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = url,
                    fontSize = 10.sp,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "youtube.com",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFFF5252)
                    )
                    Button(
                        onClick = onOpen,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Watch Lecture", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun WebReferenceCard(title: String, url: String, onOpen: () -> Unit) {
    val domain = remember(url) {
        try {
            android.net.Uri.parse(url).host?.removePrefix("www.") ?: "web resource"
        } catch (e: Exception) {
            "web resource"
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Language,
                        contentDescription = "Web link",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Text(
                        domain.uppercase(),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = title.ifBlank { domain },
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = url,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                modifier = Modifier.size(30.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Open",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
