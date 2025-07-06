package com.ticket.mdview

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.core.content.ContextCompat

class MarkdownView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    init {
        orientation = VERTICAL
    }

    fun setMarkdown(markdown: String) {
        removeAllViews()
        val lines = markdown.lines()
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            when {
                line.trim().isEmpty() -> {
                    // Пустая строка — просто отступ
                    addView(createEmptyLine())
                    i++
                }
                isHeader(line, lines, i) -> {
                    val (header, skip) = parseHeader(lines, i)
                    addView(header)
                    i += skip
                }
                line.trim().startsWith("* ") || line.trim().startsWith("- ") -> {
                    val (listView, skip) = parseUnorderedList(lines, i)
                    addView(listView)
                    i += skip
                }
                line.trim().matches(Regex("\\d+\\. .+")) -> {
                    val (listView, skip) = parseOrderedList(lines, i)
                    addView(listView)
                    i += skip
                }
                line.trim().startsWith("> ") -> {
                    val (quoteView, skip) = parseBlockQuote(lines, i)
                    addView(quoteView)
                    i += skip
                }
                line.trim().startsWith("---") || line.trim().startsWith("***") -> {
                    addView(createHorizontalRule())
                    i++
                }
                line.trim().startsWith("~~~") -> {
                    val (codeView, skip) = parseCodeBlock(lines, i)
                    addView(codeView)
                    i += skip
                }
                line.trim().startsWith("    ") -> {
                    val (codeView, skip) = parseIndentedCodeBlock(lines, i)
                    addView(codeView)
                    i += skip
                }
                line.trim().startsWith("| ") -> {
                    val (lineBlockView, skip) = parseLineBlock(lines, i)
                    addView(lineBlockView)
                    i += skip
                }
                line.trim().startsWith("!") -> {
                    addView(createImage(line))
                    i++
                }
                line.trim().startsWith("$") -> {
                    addView(createFormula(line))
                    i++
                }
                isTableLine(line) -> {
                    val (tableView, skip) = parseTableTableLayout(lines, i)
                    addView(tableView)
                    i += skip
                }
                else -> {
                    addView(createParagraph(line))
                    i++
                }
            }
        }
    }

    private fun isHeader(line: String, lines: List<String>, i: Int): Boolean {
        val trimmed = line.trim()
        if (trimmed.startsWith("#")) return true
        if (i + 1 < lines.size) {
            val next = lines[i + 1]
            if (next.trim().matches(Regex("=+")) || next.trim().matches(Regex("-+"))) return true
        }
        return false
    }

    private fun parseHeader(lines: List<String>, i: Int): Pair<TextView, Int> {
        val line = lines[i]
        val trimmed = line.trim()
        return if (trimmed.startsWith("#")) {
            val level = trimmed.takeWhile { it == '#' }.length
            val text = trimmed.drop(level).trim()
            val tv = TextView(context)
            tv.text = text
            tv.setTypeface(null, Typeface.BOLD)
            tv.textSize = when (level) {
                1 -> 26f
                2 -> 22f
                3 -> 18f
                else -> 16f
            }
            Pair(tv, 1)
        } else {
            val next = lines[i + 1]
            val level = if (next.trim().startsWith("=")) 1 else 2
            val tv = TextView(context)
            tv.text = line.trim()
            tv.setTypeface(null, Typeface.BOLD)
            tv.textSize = if (level == 1) 26f else 22f
            Pair(tv, 2)
        }
    }

    private fun createParagraph(line: String): TextView {
        val tv = TextView(context)
        tv.text = parseInline(line)
        tv.textSize = 14f
        return tv
    }

    private fun createEmptyLine(): View {
        val tv = TextView(context)
        tv.text = ""
        tv.textSize = 8f
        return tv
    }

    private fun parseInline(text: String): SpannableString {
        // Жирный **text**
        // Курсив *text*
        // Моноширинный `text`
        // Ссылки [text](url)
        // Просто базовая реализация
        val spannable = SpannableString(text)
        val bold = Regex("\\*\\*(.+?)\\*\\*")
        val italic = Regex("\\*(.+?)\\*")
        val code = Regex("`(.+?)`")
        val link = Regex("\\[(.+?)\\]\\((.+?)\\)")

        bold.findAll(text).forEach {
            spannable.setSpan(
                StyleSpan(Typeface.BOLD),
                it.range.first,
                it.range.last + 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        italic.findAll(text).forEach {
            spannable.setSpan(
                StyleSpan(Typeface.ITALIC),
                it.range.first,
                it.range.last + 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        code.findAll(text).forEach {
            spannable.setSpan(
                TypefaceSpan("monospace"),
                it.range.first,
                it.range.last + 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannable.setSpan(
                ForegroundColorSpan(ContextCompat.getColor(
                    context,
                    R.color.codeText)
                ),
                it.range.first,
                it.range.last + 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        link.findAll(text).forEach {
            spannable.setSpan(
                StyleSpan(Typeface.ITALIC),
                it.range.first,
                it.range.last + 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        return spannable
    }

    private fun parseUnorderedList(lines: List<String>, start: Int): Pair<LinearLayout, Int> {
        val layout = LinearLayout(context)
        layout.orientation = VERTICAL
        var i = start
        while (i < lines.size && (lines[i].trim().startsWith("* ") || lines[i].trim().startsWith("- "))) {
            val item = lines[i].trim().drop(2)
            val tv = TextView(context)
            tv.text = "• $item"
            tv.textSize = 14f
            layout.addView(tv)
            i++
        }
        return Pair(layout, i - start)
    }

    private fun parseOrderedList(lines: List<String>, start: Int): Pair<LinearLayout, Int> {
        val layout = LinearLayout(context)
        layout.orientation = VERTICAL
        var i = start
        var num = 1
        while (i < lines.size && lines[i].trim().matches(Regex("\\d+\\. .+"))) {
            val item = lines[i].trim().replace(Regex("\\d+\\. "), "")
            val tv = TextView(context)
            tv.text = "$num. $item"
            tv.textSize = 14f
            layout.addView(tv)
            i++
            num++
        }
        return Pair(layout, i - start)
    }

    private fun parseBlockQuote(lines: List<String>, start: Int): Pair<LinearLayout, Int> {
        val layout = LinearLayout(context)
        layout.orientation = VERTICAL
        layout.setPadding(24, 0, 0, 0)
        var i = start
        while (i < lines.size && lines[i].trim().startsWith(">")) {
            val text = lines[i].trim().drop(1).trim()
            val tv = TextView(context)
            tv.text = text
            tv.setTextColor(ContextCompat.getColor(context, R.color.quoteText))
            tv.setTypeface(null, Typeface.ITALIC)
            layout.addView(tv)
            i++
        }
        return Pair(layout, i - start)
    }

    private fun createHorizontalRule(): View {
        val v = View(context)
        v.setBackgroundColor(ContextCompat.getColor(context, R.color.hrLine))
        val params = LayoutParams(LayoutParams.MATCH_PARENT, 4)
        params.setMargins(0, 16, 0, 16)
        v.layoutParams = params
        return v
    }

    private fun parseCodeBlock(lines: List<String>, start: Int): Pair<TextView, Int> {
        val builder = StringBuilder()
        var i = start + 1
        while (i < lines.size && !lines[i].trim().startsWith("~~~")) {
            builder.append(lines[i]).append("\n")
            i++
        }
        val tv = TextView(context)
        tv.text = builder.toString().trimEnd()
        tv.setTypeface(Typeface.MONOSPACE)
        tv.setBackgroundColor(ContextCompat.getColor(context, R.color.codeBlock))
        tv.setPadding(16, 8, 16, 8)
        return Pair(tv, i - start + 1)
    }

    private fun parseIndentedCodeBlock(lines: List<String>, start: Int): Pair<TextView, Int> {
        val builder = StringBuilder()
        var i = start
        while (i < lines.size && lines[i].startsWith("    ")) {
            builder.append(lines[i].drop(4)).append("\n")
            i++
        }
        val tv = TextView(context)
        tv.text = builder.toString().trimEnd()
        tv.setTypeface(Typeface.MONOSPACE)
        tv.setBackgroundColor(ContextCompat.getColor(context, R.color.codeBlock))
        tv.setPadding(16, 8, 16, 8)
        return Pair(tv, i - start)
    }

    private fun parseLineBlock(lines: List<String>, start: Int): Pair<LinearLayout, Int> {
        val layout = LinearLayout(context)
        layout.orientation = VERTICAL
        var i = start
        while (i < lines.size && lines[i].trim().startsWith("| ")) {
            val tv = TextView(context)
            tv.text = lines[i].trim().drop(2)
            tv.setTypeface(null, Typeface.ITALIC)
            layout.addView(tv)
            i++
        }
        return Pair(layout, i - start)
    }

    private fun createImage(line: String): View {
        // ![alt](url "title")
        val regex = Regex("!\\[(.*?)\\]\\((.*?)(?:\\s+\"(.*?)\")?\\)")
        val match = regex.find(line)
        val alt = match?.groups?.get(1)?.value ?: "[image]"
        val url = match?.groups?.get(2)?.value ?: ""

        if (url.isBlank()) {
            val tv = TextView(context)
            tv.text = "[image: $alt]"
            tv.setTypeface(null, Typeface.ITALIC)
            tv.setTextColor(Color.DKGRAY)
            return tv
        }

        val imageView = androidx.appcompat.widget.AppCompatImageView(context)
        imageView.contentDescription = alt
        imageView.adjustViewBounds = true
        imageView.maxHeight = 600
        imageView.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)

        Thread {
            try {
                val bitmap = if (url.startsWith("http")) {
                    val connection = java.net.URL(url).openConnection()
                    connection.connect()
                    val input = connection.getInputStream()
                    android.graphics.BitmapFactory.decodeStream(input)
                } else if (url.startsWith("file://") || url.startsWith("/")) {
                    android.graphics.BitmapFactory.decodeFile(url.removePrefix("file://"))
                } else {
                    null
                }
                post {
                    if (bitmap != null) {
                        imageView.setImageBitmap(bitmap)
                    } else {
                        val tv = TextView(context)
                        tv.text = "[image: $alt]"
                        tv.setTypeface(null, Typeface.ITALIC)
                        tv.setTextColor(Color.DKGRAY)
                        // Заменяем ImageView на TextView
                        val parent = imageView.parent as? LinearLayout
                        val idx = parent?.indexOfChild(imageView) ?: -1
                        if (parent != null && idx >= 0) {
                            parent.removeViewAt(idx)
                            parent.addView(tv, idx)
                        }
                    }
                }
            } catch (e: Exception) {
                post {
                    val tv = TextView(context)
                    tv.text = "[image: $alt]"
                    tv.setTypeface(null, Typeface.ITALIC)
                    tv.setTextColor(Color.DKGRAY)
                    val parent = imageView.parent as? LinearLayout
                    val idx = parent?.indexOfChild(imageView) ?: -1
                    if (parent != null && idx >= 0) {
                        parent.removeViewAt(idx)
                        parent.addView(tv, idx)
                    }
                }
            }
        }.start()
        return imageView
    }

    private fun createFormula(line: String): TextView {
        val tv = TextView(context)
        tv.text = line
        tv.setTextColor(ContextCompat.getColor(context, R.color.formulaText))
        tv.setTypeface(null, Typeface.ITALIC)
        return tv
    }

    private fun isTableLine(line: String): Boolean {
        return line.contains("|") || (line.contains("  ") && line.contains("---"))
    }

    private fun parseTableTableLayout(lines: List<String>, start: Int): Pair<TableLayout, Int> {
        val table = TableLayout(context)
        table.setBackgroundColor(ContextCompat.getColor(context, R.color.tableBg))
        table.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        val rows = mutableListOf<List<String>>()
        var i = start
        var columnsCount = 0
        var colStarts: IntArray? = null

        while (i < lines.size && (lines[i].contains("|") || lines[i].contains("  "))) {
            val line = lines[i].trimEnd()
            if (line.isEmpty()) break
            if (line.matches(Regex("^[-| ]+$"))) {
                i++
                continue
            }
            if (line.contains("|")) {
                val cells = line.split("|").map { it.trim() }
                if (cells.size > columnsCount) columnsCount = cells.size
                rows.add(cells)
            } else {
                if (colStarts == null) {
                    val matcher = Regex("\\S+").findAll(line)
                    colStarts = matcher.map { it.range.first }.toList().toIntArray()
                    columnsCount = colStarts.size
                }
                val cells = mutableListOf<String>()
                for (c in 0 until columnsCount) {
                    val startC = colStarts[c]
                    val end = if (c + 1 < columnsCount) colStarts[c + 1] else line.length
                    val cell = if (startC < line.length) line.substring(startC, end).trim() else ""
                    cells.add(cell)
                }
                rows.add(cells)
            }
            i++

            while (i < lines.size && lines[i].startsWith(" ")) {
                val cont = lines[i].trimEnd()
                if (rows.isNotEmpty()) {
                    val lastRow = rows.last().toMutableList()
                    if (lastRow.isNotEmpty()) {
                        lastRow[lastRow.size - 1] = lastRow.last() + "\n" + cont.trim()
                        rows[rows.size - 1] = lastRow
                    }
                }
                i++
            }
        }

        val paint = TextView(context).paint
        val maxWidths = IntArray(columnsCount) { 0 }
        for (row in rows) {
            for (j in row.indices) {
                val width = paint.measureText(row[j]).toInt()
                if (width > maxWidths[j]) maxWidths[j] = width
            }
        }

        for (j in maxWidths.indices) {
            maxWidths[j] += 32
        }

        var headerParsed = false
        for (rowCells in rows) {
            val row = TableRow(context)
            row.layoutParams = TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT,
                TableLayout.LayoutParams.WRAP_CONTENT
            )
            for (j in 0 until columnsCount) {
                val tv = TextView(context)
                val text = if (j < rowCells.size) rowCells[j] else ""
                tv.text = text
                tv.setPadding(8, 8, 8, 8)
                tv.gravity = Gravity.CENTER
                tv.setSingleLine(false)
                tv.maxLines = Integer.MAX_VALUE
                tv.ellipsize = null
                if (!headerParsed) tv.setTypeface(null, Typeface.BOLD)
                val params = TableRow.LayoutParams(maxWidths[j], TableRow.LayoutParams.WRAP_CONTENT)
                tv.layoutParams = params
                row.addView(tv)
            }
            table.addView(row)
            if (!headerParsed) headerParsed = true
        }
        return Pair(table, rows.size)
    }
}