package com.ronin.ai.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ronin.ai.core.design.theme.RoninAmber
import com.ronin.ai.core.design.theme.RoninBlack
import com.ronin.ai.core.design.theme.RoninBorder
import com.ronin.ai.core.design.theme.RoninCyan
import com.ronin.ai.core.design.theme.RoninTextSecondary

// ---------------------------------------------------------------------------
// Lightweight Markdown renderer.
//
// A full Markdown library would add a large dependency for very little gain on
// a low-end device, so RONIN parses the subset that language models actually
// emit: fenced code blocks, headings, bullet/numbered lists, blockquotes and
// inline bold / italic / `code` / links.
// ---------------------------------------------------------------------------

private sealed interface MdBlock {
    data class Paragraph(val text: String) : MdBlock
    data class Heading(val text: String, val level: Int) : MdBlock
    data class Bullet(val text: String, val ordinal: String?) : MdBlock
    data class Quote(val text: String) : MdBlock
    data class Code(val code: String, val language: String?) : MdBlock
}

private fun parseMarkdown(source: String): List<MdBlock> {
    val blocks = mutableListOf<MdBlock>()
    val lines = source.replace("\r\n", "\n").split("\n")
    var i = 0
    val paragraph = StringBuilder()

    fun flushParagraph() {
        if (paragraph.isNotBlank()) blocks += MdBlock.Paragraph(paragraph.toString().trim())
        paragraph.setLength(0)
    }

    while (i < lines.size) {
        val line = lines[i]
        val trimmed = line.trim()

        // Fenced code block ```lang
        if (trimmed.startsWith("```")) {
            flushParagraph()
            val language = trimmed.removePrefix("```").trim().ifBlank { null }
            val code = StringBuilder()
            i++
            while (i < lines.size && !lines[i].trim().startsWith("```")) {
                code.appendLine(lines[i])
                i++
            }
            i++ // consume closing fence
            blocks += MdBlock.Code(code.toString().trimEnd('\n'), language)
            continue
        }

        when {
            trimmed.isEmpty() -> flushParagraph()

            trimmed.startsWith("#") -> {
                flushParagraph()
                val level = trimmed.takeWhile { it == '#' }.length.coerceAtMost(3)
                blocks += MdBlock.Heading(trimmed.drop(level).trim(), level)
            }

            trimmed.startsWith("> ") -> {
                flushParagraph()
                blocks += MdBlock.Quote(trimmed.removePrefix("> ").trim())
            }

            trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("+ ") -> {
                flushParagraph()
                blocks += MdBlock.Bullet(trimmed.drop(2).trim(), null)
            }

            Regex("""^\d+[.)]\s+.*""").matches(trimmed) -> {
                flushParagraph()
                val ordinal = trimmed.takeWhile { it.isDigit() }
                blocks += MdBlock.Bullet(
                    trimmed.dropWhile { it.isDigit() }.drop(1).trim(),
                    ordinal
                )
            }

            else -> {
                if (paragraph.isNotEmpty()) paragraph.append(' ')
                paragraph.append(trimmed)
            }
        }
        i++
    }
    flushParagraph()
    return blocks
}

/**
 * Applies inline **bold**, *italic*, `code`, ~~strike~~ and [links](url).
 *
 * The base colour is pushed *first* so that spans which set their own colour
 * (code, links) still win — applying it afterwards would flatten them.
 */
private fun inlineMarkdown(text: String, baseColor: Color): AnnotatedString =
    buildAnnotatedString {
        pushStyle(SpanStyle(color = baseColor))
        // Ordered so that ** is matched before *.
        val pattern = Regex(
            """(\*\*|__)(.+?)\1|(\*|_)(.+?)\3|`([^`]+)`|~~(.+?)~~|\[([^]]+)]\(([^)]+)\)"""
        )
        var cursor = 0
        for (match in pattern.findAll(text)) {
            if (match.range.first > cursor) {
                append(text.substring(cursor, match.range.first))
            }
            val g = match.groupValues
            when {
                g[2].isNotEmpty() -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(g[2]) }
                g[4].isNotEmpty() -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(g[4]) }
                g[5].isNotEmpty() -> withStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = RoninBlack.copy(alpha = 0.6f),
                        color = RoninAmber
                    )
                ) { append(g[5]) }
                g[6].isNotEmpty() -> withStyle(
                    SpanStyle(textDecoration = TextDecoration.LineThrough)
                ) { append(g[6]) }
                g[7].isNotEmpty() -> withStyle(
                    SpanStyle(color = RoninCyan, textDecoration = TextDecoration.Underline)
                ) { append(g[7]) }
                else -> append(match.value)
            }
            cursor = match.range.last + 1
        }
        if (cursor < text.length) append(text.substring(cursor))
        pop()
    }

/**
 * Renders a Markdown-ish assistant reply. Code blocks get their own surface
 * with a copy button, matching the "copy response" requirement.
 */
@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    val blocks = remember(text) { parseMarkdown(text) }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        blocks.forEach { block ->
            when (block) {
                is MdBlock.Paragraph -> Text(
                    text = inlineMarkdown(block.text, color),
                    style = MaterialTheme.typography.bodyMedium
                )

                is MdBlock.Heading -> Text(
                    text = inlineMarkdown(block.text, color),
                    style = when (block.level) {
                        1 -> MaterialTheme.typography.titleLarge
                        2 -> MaterialTheme.typography.titleMedium
                        else -> MaterialTheme.typography.bodyLarge
                    },
                    fontWeight = FontWeight.Bold
                )

                is MdBlock.Bullet -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = block.ordinal?.let { "$it." } ?: "•",
                        style = MaterialTheme.typography.bodyMedium,
                        color = RoninCyan
                    )
                    Text(
                        text = inlineMarkdown(block.text, color),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                }

                is MdBlock.Quote -> Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(RoninCyan.copy(alpha = 0.06f), RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("▍", color = RoninCyan, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = inlineMarkdown(block.text, RoninTextSecondary),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                is MdBlock.Code -> CodeBlock(block.code, block.language)
            }
        }
    }
}

@Composable
private fun CodeBlock(code: String, language: String?) {
    val clipboard = LocalClipboardManager.current
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(RoninBlack.copy(alpha = 0.75f), shape)
            .border(1.dp, RoninBorder, shape)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 4.dp, top = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = (language ?: "code").uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = RoninTextSecondary,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { clipboard.setText(AnnotatedString(code)) }) {
                Icon(
                    Icons.Rounded.ContentCopy,
                    contentDescription = "Copy code",
                    tint = RoninCyan,
                    modifier = Modifier.padding(2.dp)
                )
            }
        }
        Text(
            text = code,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp
            ),
            color = RoninAmber,
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp)
                .padding(bottom = 12.dp)
        )
    }
}
