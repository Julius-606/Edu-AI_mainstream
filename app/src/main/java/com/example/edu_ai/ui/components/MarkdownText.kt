
package com.example.edu_ai.ui.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle

@Composable
fun FormattedText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    onLinkClicked: ((String) -> Unit)? = null
) {
    val annotatedString = parseMarkdown(text)
    Text(
        text = annotatedString,
        modifier = modifier,
        style = style,
        maxLines = maxLines,
        overflow = overflow
    )
}

/**
 * A simple Markdown parser for Compose.
 * Removes markers like **, *, and ` while applying styles.
 */
fun parseMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        val boldRegex = Regex("""\*\*(.*?)\*\*""", RegexOption.DOT_MATCHES_ALL)
        val italicRegex = Regex("""\*(.*?)\*""", RegexOption.DOT_MATCHES_ALL)
        val codeRegex = Regex("""`(.*?)`""", RegexOption.DOT_MATCHES_ALL)
        
        val tokens = mutableListOf<Token>()
        
        // Find all bold matches
        boldRegex.findAll(text).forEach { 
            tokens.add(Token(it.range, "bold", it.groupValues[1])) 
        }
        
        // Find all italic matches, avoiding overlaps with bold
        italicRegex.findAll(text).forEach { match ->
            if (tokens.none { it.range.contains(match.range.first) || it.range.contains(match.range.last) }) {
                tokens.add(Token(match.range, "italic", match.groupValues[1]))
            }
        }
        
        // Find all code matches, avoiding overlaps
        codeRegex.findAll(text).forEach { match ->
            if (tokens.none { it.range.contains(match.range.first) || it.range.contains(match.range.last) }) {
                tokens.add(Token(match.range, "code", match.groupValues[1]))
            }
        }
        
        tokens.sortBy { it.range.first }
        
        var lastIndex = 0
        for (token in tokens) {
            // Append text before the token
            if (token.range.first > lastIndex) {
                append(text.substring(lastIndex, token.range.first))
            }
            
            // Apply style and append content (without markers)
            val spanStyle = when (token.type) {
                "bold" -> SpanStyle(fontWeight = FontWeight.Bold)
                "italic" -> SpanStyle(fontStyle = FontStyle.Italic)
                "code" -> SpanStyle(
                    fontFamily = FontFamily.Monospace,
                    background = Color.LightGray.copy(alpha = 0.3f)
                )
                else -> SpanStyle()
            }
            
            withStyle(spanStyle) {
                append(token.content)
            }
            
            lastIndex = token.range.last + 1
        }
        
        // Append remaining text
        if (lastIndex < text.length) {
            append(text.substring(lastIndex))
        }
    }
}

data class Token(val range: IntRange, val type: String, val content: String)


 