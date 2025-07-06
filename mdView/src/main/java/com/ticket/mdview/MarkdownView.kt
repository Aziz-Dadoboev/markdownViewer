package com.ticket.mdview

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.text.style.UnderlineSpan
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
            // Попытка распознать простую таблицу
            val (table, skipT) = tryParseSimpleTable(lines, i)
            if (table != null) {
                addView(table)
                i += skipT
                continue
            }
            when {
                line.trim().isEmpty() -> {
                    addView(createEmptyLine())
                    i++
                }
                line.trim().startsWith("    ") -> {
                    val (codeView, skip) = parseIndentedCodeBlock(lines, i)
                    addView(codeView)
                    i += skip
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
                line.trim().startsWith("---") -> {
                    addView(createHorizontalRule())
                    i++
                }
                line.trim().startsWith("~~~") -> {
                    val (codeView, skip) = parseCodeBlock(lines, i)
                    addView(codeView)
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
                4 -> 16f
                5 -> 15f
                6 -> 14f
                else -> 14f
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
        tv.text = parseInline(line, tv)
        tv.textSize = 14f
        tv.movementMethod = LinkMovementMethod.getInstance()
        return tv
    }

    private fun createEmptyLine(): View {
        val tv = TextView(context)
        tv.text = ""
        tv.textSize = 8f
        return tv
    }

    private fun parseInline(text: String, textView: TextView? = null): CharSequence {
        val result = MarkdownParser.parseInline(text)
        val builder = android.text.SpannableStringBuilder(result.text)
        for (span in result.spans) {
            when (span.type) {
                MarkdownSpan.Type.BOLD_ITALIC -> builder
                    .setSpan(StyleSpan(Typeface.BOLD_ITALIC), span.start, span.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                MarkdownSpan.Type.BOLD -> builder
                    .setSpan(StyleSpan(Typeface.BOLD), span.start, span.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                MarkdownSpan.Type.ITALIC -> builder
                    .setSpan(StyleSpan(Typeface.ITALIC), span.start, span.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                MarkdownSpan.Type.CODE -> builder
                    .setSpan(TypefaceSpan("monospace"), span.start, span.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                MarkdownSpan.Type.STRIKETHROUGH -> builder
                    .setSpan(StrikethroughSpan(), span.start, span.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                MarkdownSpan.Type.UNDERLINE -> builder
                    .setSpan(UnderlineSpan(), span.start, span.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                MarkdownSpan.Type.LINK -> if (textView != null && span.url != null) {
                    builder.setSpan(object : ClickableSpan() {
                        override fun onClick(widget: View) {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(span.url))
                                widget.context.startActivity(intent)
                            } catch (_: Exception) {}
                        }
                        override fun updateDrawState(ds: TextPaint) {
                            super.updateDrawState(ds)
                            ds.isUnderlineText = true
                            ds.color = ds.linkColor
                        }
                    }, span.start, span.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }

            }
        }
        return builder
    }

    private fun parseUnorderedList(lines: List<String>, start: Int): Pair<LinearLayout, Int> {
        val layout = LinearLayout(context)
        layout.orientation = VERTICAL
        var i = start
        while (i < lines.size && (lines[i].trim().startsWith("* ") || lines[i].trim().startsWith("- "))) {
            val item = lines[i].trim().drop(2)
            val tv = TextView(context)
            tv.text = context.getString(R.string.item, item)
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
            tv.text = context.getString(R.string.num_item, num, item)
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

    private fun createImage(line: String): View {
        // ![alt](url "title")
        val regex = Regex("!\\[(.*?)]\\((.*?)(?:\\s+\"(.*?)\")?\\)")
        val match = regex.find(line)
        val alt = match?.groups?.get(1)?.value ?: "[image]"
        val url = match?.groups?.get(2)?.value ?: ""

        if (url.isBlank()) {
            val tv = TextView(context)
            tv.text = context.getString(R.string.image, alt)
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
                        tv.text = context.getString(R.string.image, alt)
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
            } catch (e: Exception) {
                post {
                    val tv = TextView(context)
                    tv.text = context.getString(R.string.image, alt)
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

    private fun tryParseSimpleTable(lines: List<String>, start: Int): Pair<TableLayout?, Int> {
        var i = start
        if (i + 2 > lines.size) return Pair(null, 0)
        val headerLine = lines[i].trim()
        val sepLine = lines.getOrNull(i + 1)?.trim() ?: return Pair(null, 0)
        if (!headerLine.startsWith("|") || !headerLine.endsWith("|")) return Pair(null, 0)
        if (!sepLine.startsWith("|") || !sepLine.endsWith("|")) return Pair(null, 0)
        if (!sepLine.replace("-", "").replace("|", "").trim().isEmpty()) return Pair(null, 0)
        val headers = headerLine.split("|").map { it.trim() }.filter { it.isNotEmpty() }
        val columnsCount = headers.size

        val dataRows = mutableListOf<List<String>>()
        i += 2
        while (i < lines.size) {
            val line = lines[i].trim()
            if (!line.startsWith("|") || !line.endsWith("|")) break
            val cells = line.split("|").map { it.trim() }.filter { it.isNotEmpty() }
            if (cells.size != columnsCount) break
            dataRows.add(cells)
            i++
        }
        if (dataRows.isEmpty()) return Pair(null, 0)
        val table = TableLayout(context)
        table.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        // Заголовок
        val headerRow = TableRow(context)
        for (h in headers) {
            val tv = TextView(context)
            tv.text = h
            tv.setTypeface(null, Typeface.BOLD)
            tv.setPadding(8, 8, 8, 8)
            tv.gravity = Gravity.CENTER
            headerRow.addView(tv)
        }
        table.addView(headerRow)
        // Данные
        for (row in dataRows) {
            val tableRow = TableRow(context)
            for (cell in row) {
                val tv = TextView(context)
                tv.text = cell
                tv.setPadding(8, 8, 8, 8)
                tv.gravity = Gravity.CENTER
                tableRow.addView(tv)
            }
            table.addView(tableRow)
        }
        return Pair(table, 2 + dataRows.size)
    }
}