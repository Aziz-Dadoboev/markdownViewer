package com.ticket.mdview

data class MarkdownSpan(val type: Type, val start: Int, val end: Int, val url: String? = null) {
    enum class Type { BOLD_ITALIC, BOLD, ITALIC, CODE, LINK, STRIKETHROUGH, UNDERLINE }
}

data class MarkdownParseResult(val text: String, val spans: List<MarkdownSpan>)

object MarkdownParser {
    fun parseInline(text: String, offset: Int = 0): MarkdownParseResult {
        data class Marker(
            val type: MarkdownSpan.Type,
            val open: String,
            val close: String,
            val regex: Regex,
            val urlGroup: Int? = null
        )
        val markers = listOf(
            Marker(MarkdownSpan.Type.BOLD_ITALIC, "***", "***", Regex("\\*\\*\\*(.+?)\\*\\*\\*")),
            Marker(MarkdownSpan.Type.LINK, "[", ")", Regex("\\[(.+?)]\\((.+?)\\)"), urlGroup = 2),
            Marker(MarkdownSpan.Type.STRIKETHROUGH, "~~", "~~", Regex("~~(.+?)~~")),
            Marker(MarkdownSpan.Type.UNDERLINE, "__", "__", Regex("__([^_]+?)__")),
            Marker(MarkdownSpan.Type.BOLD, "**", "**", Regex("\\*\\*(.+?)\\*\\*")),
            Marker(MarkdownSpan.Type.ITALIC, "*", "*", Regex("(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)")),
            Marker(MarkdownSpan.Type.CODE, "`", "`", Regex("`(.+?)`"))
        )

        var firstMatch: MatchResult? = null
        var firstMarker: Marker? = null
        var firstIndex = text.length
        for (marker in markers) {
            val match = marker.regex.find(text)
            if (match != null && match.range.first < firstIndex) {
                firstMatch = match
                firstMarker = marker
                firstIndex = match.range.first
            }
        }
        if (firstMatch == null || firstMarker == null) {
            return MarkdownParseResult(text, emptyList())
        }
        val before = text.substring(0, firstMatch.range.first)
        val inner = firstMatch.groupValues[1]
        val after = text.substring(firstMatch.range.last + 1)
        val url = firstMarker.urlGroup?.let { g -> firstMatch.groupValues.getOrNull(g) }

        val innerResult = parseInline(inner, offset + before.length)
        val afterResult = parseInline(after, offset + before.length + innerResult.text.length)

        val resultText = before + innerResult.text + afterResult.text
        val spans = mutableListOf<MarkdownSpan>()
        val spanStart = before.length + offset
        val spanEnd = spanStart + innerResult.text.length
        if (firstMarker.type == MarkdownSpan.Type.LINK) {
            spans.add(MarkdownSpan(firstMarker.type, spanStart, spanEnd, url))
        } else if (firstMarker.type == MarkdownSpan.Type.BOLD_ITALIC) {
            spans.add(MarkdownSpan(MarkdownSpan.Type.BOLD, spanStart, spanEnd))
            spans.add(MarkdownSpan(MarkdownSpan.Type.ITALIC, spanStart, spanEnd))
        } else {
            spans.add(MarkdownSpan(firstMarker.type, spanStart, spanEnd))
        }
        spans.addAll(innerResult.spans)
        spans.addAll(afterResult.spans)
        return MarkdownParseResult(resultText, spans.sortedBy { it.start })
    }
} 