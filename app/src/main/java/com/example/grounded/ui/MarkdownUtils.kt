package com.example.grounded.ui

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.withStyle

// ---------------------------------------------------------------------------
// Rendering
// ---------------------------------------------------------------------------

/**
 * Converts a subset of Markdown to an [AnnotatedString] for display.
 *
 * Supported syntax:
 *   - `**text**`   → bold
 *   - `*text*`     → italic
 *   - `- text` or `* text` at line start → bullet "• text"
 *   - `1. text` (any digit + dot + space) at line start → kept as-is
 */
fun renderMarkdown(text: String): AnnotatedString = buildAnnotatedString {
    val lines = text.lines()
    lines.forEachIndexed { idx, rawLine ->
        if (idx > 0) append('\n')
        val (prefix, content) = extractLinePrefix(rawLine)
        append(prefix)
        appendInlineMarkdown(content)
    }
}

private fun extractLinePrefix(line: String): Pair<String, String> = when {
    line.startsWith("- ") -> "• " to line.drop(2)
    line.startsWith("* ") && !line.startsWith("**") -> "• " to line.drop(2)
    line.isNotEmpty() && line[0].isDigit() -> {
        val dotSpace = line.indexOf(". ")
        if (dotSpace > 0 && line.substring(0, dotSpace).all { it.isDigit() }) {
            line.substring(0, dotSpace + 2) to line.substring(dotSpace + 2)
        } else "" to line
    }
    else -> "" to line
}

private fun AnnotatedString.Builder.appendInlineMarkdown(text: String) {
    var i = 0
    while (i < text.length) {
        when {
            text.startsWith("**", i) -> {
                val closeIdx = text.indexOf("**", i + 2)
                if (closeIdx >= 0) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        appendInlineMarkdown(text.substring(i + 2, closeIdx))
                    }
                    i = closeIdx + 2
                } else {
                    append(text[i]); i++
                }
            }
            text.startsWith("*", i) -> {
                val closeIdx = text.indexOf("*", i + 1)
                if (closeIdx >= 0) {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        appendInlineMarkdown(text.substring(i + 1, closeIdx))
                    }
                    i = closeIdx + 1
                } else {
                    append(text[i]); i++
                }
            }
            else -> {
                append(text[i]); i++
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Active-state detection (toolbar highlight)
// ---------------------------------------------------------------------------

fun hasActiveBold(tfv: TextFieldValue): Boolean {
    val before = textBeforeCursorOnLine(tfv)
    var boldCount = 0
    var i = 0
    while (i < before.length) {
        when {
            // *** → bold+italic toggle; count bold
            i + 2 < before.length && before[i] == '*' && before[i + 1] == '*' && before[i + 2] == '*' -> {
                boldCount++; i += 3
            }
            // ** → bold toggle
            i + 1 < before.length && before[i] == '*' && before[i + 1] == '*' -> {
                boldCount++; i += 2
            }
            // lone * → italic only, skip
            before[i] == '*' -> i++
            else -> i++
        }
    }
    return boldCount % 2 == 1
}

fun hasActiveItalic(tfv: TextFieldValue): Boolean {
    val before = textBeforeCursorOnLine(tfv)
    var italicCount = 0
    var i = 0
    while (i < before.length) {
        when {
            i + 2 < before.length && before[i] == '*' && before[i + 1] == '*' && before[i + 2] == '*' -> {
                italicCount++; i += 3
            }
            i + 1 < before.length && before[i] == '*' && before[i + 1] == '*' -> {
                i += 2 // bold marker, not italic
            }
            before[i] == '*' -> {
                italicCount++; i++
            }
            else -> i++
        }
    }
    return italicCount % 2 == 1
}

fun hasActiveBullet(tfv: TextFieldValue): Boolean =
    currentLine(tfv).startsWith("- ") ||
        (currentLine(tfv).startsWith("* ") && !currentLine(tfv).startsWith("**"))

fun hasActiveNumbered(tfv: TextFieldValue): Boolean {
    val line = currentLine(tfv)
    if (line.isEmpty() || !line[0].isDigit()) return false
    val dotSpace = line.indexOf(". ")
    return dotSpace > 0 && line.substring(0, dotSpace).all { it.isDigit() }
}

// ---------------------------------------------------------------------------
// Apply formatting
// ---------------------------------------------------------------------------

fun applyBold(tfv: TextFieldValue): TextFieldValue =
    applyInlineMarker(tfv, "**")

fun applyItalic(tfv: TextFieldValue): TextFieldValue =
    applyInlineMarker(tfv, "*")

fun applyBullet(tfv: TextFieldValue): TextFieldValue =
    applyLinePrefix(tfv, "- ")

fun applyNumbered(tfv: TextFieldValue): TextFieldValue =
    applyLinePrefix(tfv, "1. ")

// ---------------------------------------------------------------------------
// Private helpers
// ---------------------------------------------------------------------------

private fun applyInlineMarker(tfv: TextFieldValue, marker: String): TextFieldValue {
    val text = tfv.text
    val start = tfv.selection.min
    val end = tfv.selection.max
    val mLen = marker.length

    return if (start == end) {
        // No selection → insert marker pair and position cursor inside
        val newText = text.substring(0, start) + marker + marker + text.substring(start)
        tfv.copy(text = newText, selection = TextRange(start + mLen))
    } else {
        val selected = text.substring(start, end)
        val wrappedOutside = start >= mLen && end + mLen <= text.length &&
            text.substring(start - mLen, start) == marker &&
            text.substring(end, end + mLen) == marker
        val wrappedInside = selected.startsWith(marker) && selected.endsWith(marker) &&
            selected.length >= mLen * 2

        when {
            wrappedOutside -> {
                // Unwrap markers that are outside the selection
                val newText = text.substring(0, start - mLen) + selected + text.substring(end + mLen)
                tfv.copy(
                    text = newText,
                    selection = TextRange(start - mLen, start - mLen + selected.length)
                )
            }
            wrappedInside -> {
                // Unwrap markers inside the selection
                val inner = selected.drop(mLen).dropLast(mLen)
                val newText = text.substring(0, start) + inner + text.substring(end)
                tfv.copy(text = newText, selection = TextRange(start, start + inner.length))
            }
            else -> {
                // Wrap selection with markers
                val newText = text.substring(0, start) + marker + selected + marker + text.substring(end)
                tfv.copy(text = newText, selection = TextRange(start + mLen, end + mLen))
            }
        }
    }
}

private fun applyLinePrefix(tfv: TextFieldValue, prefix: String): TextFieldValue {
    val text = tfv.text
    val pos = tfv.selection.start
    val lineStart = lineStartOf(text, pos)
    val lineEnd = lineEndOf(text, pos)
    val line = text.substring(lineStart, lineEnd)

    // Determine current line prefix state
    val hasBullet = line.startsWith("- ") || (line.startsWith("* ") && !line.startsWith("**"))
    val hasNumbered = line.isNotEmpty() && line[0].isDigit() && run {
        val ds = line.indexOf(". "); ds > 0 && line.substring(0, ds).all { it.isDigit() }
    }
    val existingPrefixLen = when {
        hasBullet -> 2
        hasNumbered -> line.indexOf(". ") + 2
        else -> 0
    }
    val hasThisPrefix = line.startsWith(prefix)

    return if (hasThisPrefix) {
        // Remove this prefix
        val newText = text.substring(0, lineStart) + line.drop(prefix.length) + text.substring(lineEnd)
        val newCursor = (pos - prefix.length).coerceAtLeast(lineStart)
        tfv.copy(text = newText, selection = TextRange(newCursor))
    } else {
        // Replace any existing prefix (bullet→numbered or vice-versa), or add fresh
        val strippedLine = line.drop(existingPrefixLen)
        val newText = text.substring(0, lineStart) + prefix + strippedLine + text.substring(lineEnd)
        val cursorShift = prefix.length - existingPrefixLen
        val newCursor = (pos + cursorShift).coerceAtLeast(lineStart)
        tfv.copy(text = newText, selection = TextRange(newCursor))
    }
}

private fun textBeforeCursorOnLine(tfv: TextFieldValue): String {
    val text = tfv.text
    val pos = tfv.selection.min
    val lineStart = lineStartOf(text, pos)
    return text.substring(lineStart, pos)
}

private fun currentLine(tfv: TextFieldValue): String {
    val text = tfv.text
    val pos = tfv.selection.min
    return text.substring(lineStartOf(text, pos), lineEndOf(text, pos))
}

private fun lineStartOf(text: String, pos: Int): Int {
    if (pos == 0) return 0
    val nl = text.lastIndexOf('\n', pos - 1)
    return if (nl < 0) 0 else nl + 1
}

private fun lineEndOf(text: String, pos: Int): Int {
    val nl = text.indexOf('\n', pos)
    return if (nl < 0) text.length else nl
}
