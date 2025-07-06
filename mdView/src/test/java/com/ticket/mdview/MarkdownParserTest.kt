package com.ticket.mdview

import org.junit.Assert.*
import org.junit.Test

class MarkdownParserTest {
    @Test
    fun testBold() {
        val result = MarkdownParser.parseInline("Это **жирный** текст")
        assertEquals("Это жирный текст", result.text)
        assertEquals(1, result.spans.size)
        val span = result.spans[0]
        assertEquals(MarkdownSpan.Type.BOLD, span.type)
        assertEquals(4, span.start)
        assertEquals(10, span.end)
    }

    @Test
    fun testItalic() {
        val result = MarkdownParser.parseInline("Это *курсив* текст")
        assertEquals("Это курсив текст", result.text)
        assertEquals(1, result.spans.size)
        val span = result.spans[0]
        assertEquals(MarkdownSpan.Type.ITALIC, span.type)
        assertEquals(4, span.start)
        assertEquals(10, span.end)
    }

    @Test
    fun testBoldItalic() {
        val result = MarkdownParser.parseInline("Это ***жирный и одновременно курсив*** текст")
        assertEquals("Это жирный и одновременно курсив текст", result.text)
        val bold = result.spans.find { it.type == MarkdownSpan.Type.BOLD }!!
        val italic = result.spans.find { it.type == MarkdownSpan.Type.ITALIC }!!
        assertEquals(4, bold.start)
        assertEquals(32, bold.end)
        assertEquals(4, italic.start)
        assertEquals(32, italic.end)
    }

    @Test
    fun testCode() {
        val result = MarkdownParser.parseInline("Это код `val s = \"hello world!\"` внутри текста")
        assertEquals("Это код val s = \"hello world!\" внутри текста", result.text)
        assertEquals(1, result.spans.size)
        val span = result.spans[0]
        assertEquals(MarkdownSpan.Type.CODE, span.type)
        assertEquals(8, span.start)
        assertEquals(30, span.end)
    }

    @Test
    fun testLink() {
        val result = MarkdownParser.parseInline("Ссылка на [Google](https://google.com)")
        assertEquals("Ссылка на Google", result.text)
        assertEquals(1, result.spans.size)
        val span = result.spans[0]
        assertEquals(MarkdownSpan.Type.LINK, span.type)
        assertEquals(10, span.start)
        assertEquals(16, span.end)
        assertEquals("https://google.com", span.url)
    }

    @Test
    fun testStrikethrough() {
        val result = MarkdownParser.parseInline("Это ~~зачёркнутый~~ текст")
        assertEquals("Это зачёркнутый текст", result.text)
        assertEquals(1, result.spans.size)
        val span = result.spans[0]
        assertEquals(MarkdownSpan.Type.STRIKETHROUGH, span.type)
        assertEquals(4, span.start)
        assertEquals(15, span.end)
    }

    @Test
    fun testUnderline() {
        val result = MarkdownParser.parseInline("Это __подчёркнутый__ текст")
        assertEquals("Это подчёркнутый текст", result.text)
        assertEquals(1, result.spans.size)
        val span = result.spans[0]
        assertEquals(MarkdownSpan.Type.UNDERLINE, span.type)
        assertEquals(4, span.start)
        assertEquals(16, span.end)
    }

    @Test
    fun testNestedStyles() {
        val result = MarkdownParser.parseInline("**жирный и *курсив* внутри**")
        assertEquals("жирный и курсив внутри", result.text)
        assertEquals(2, result.spans.size)
        val bold = result.spans.find { it.type == MarkdownSpan.Type.BOLD }!!
        assertEquals(0, bold.start)
        assertEquals(22, bold.end)
        val italic = result.spans.find { it.type == MarkdownSpan.Type.ITALIC }!!
        assertEquals(9, italic.start)
        assertEquals(15, italic.end)
    }

    @Test
    fun testMultipleStyles() {
        val result = MarkdownParser.parseInline("Это **жирный** и *курсив* и ~~зачёркнутый~~ текст")
        assertEquals("Это жирный и курсив и зачёркнутый текст", result.text)
        assertEquals(3, result.spans.size)
        assertTrue(result.spans.any { it.type == MarkdownSpan.Type.BOLD })
        assertTrue(result.spans.any { it.type == MarkdownSpan.Type.ITALIC })
        assertTrue(result.spans.any { it.type == MarkdownSpan.Type.STRIKETHROUGH })
    }

    @Test
    fun testUnclosedMarker() {
        val result = MarkdownParser.parseInline("Это **не закрытый жирный")
        assertEquals("Это **не закрытый жирный", result.text)
        assertTrue(result.spans.isEmpty())
    }

    @Test
    fun testMultipleLinks() {
        val result = MarkdownParser.parseInline("Ссылки: [A](a.com) и [B](b.com)")
        assertEquals("Ссылки: A и B", result.text)
        assertEquals(2, result.spans.size)
        val sortedSpans = result.spans.sortedBy { it.start }
        val a = sortedSpans[0]
        val b = sortedSpans[1]
        assertEquals(MarkdownSpan.Type.LINK, a.type)
        assertEquals(MarkdownSpan.Type.LINK, b.type)
        assertEquals("a.com", a.url)
        assertEquals("b.com", b.url)
    }

    @Test
    fun testEmptyString() {
        val result = MarkdownParser.parseInline("")
        assertEquals("", result.text)
        assertTrue(result.spans.isEmpty())
    }

    @Test
    fun testPlainText() {
        val result = MarkdownParser.parseInline("Обычный текст без markdown")
        assertEquals("Обычный текст без markdown", result.text)
        assertTrue(result.spans.isEmpty())
    }

    @Test
    fun testHeaderLevels() {
        val headers = listOf(
            "# Заголовок 1" to 1,
            "## Заголовок 2" to 2,
            "### Заголовок 3" to 3,
            "#### Заголовок 4" to 4,
            "##### Заголовок 5" to 5,
            "###### Заголовок 6" to 6
        )
        headers.forEach { (src, level) ->
            val result = MarkdownParser.parseInline(src)
            val expected = src.drop(level).trimStart()
            assertTrue(result.text.endsWith(expected))
            assertTrue(result.spans.isEmpty())
        }
    }

    @Test
    fun testHeaderWithInlineStyle() {
        val src = "### Заголовок с **жирным** и *курсивом*"
        val result = MarkdownParser.parseInline(src)
        val expected = "Заголовок с жирным и курсивом"
        assertTrue(result.text.endsWith(expected))
        assertEquals(2, result.spans.size)
        assertTrue(result.spans.any { it.type == MarkdownSpan.Type.BOLD })
        assertTrue(result.spans.any { it.type == MarkdownSpan.Type.ITALIC })
    }
} 