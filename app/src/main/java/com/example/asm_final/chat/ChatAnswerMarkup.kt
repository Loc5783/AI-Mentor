package com.example.asm_final.chat

/** Small, safe Markdown subset for AI answers. Math is left intact for the bundled KaTeX renderer. */
object ChatAnswerMarkup {
    private val math = Regex("""(?s)\$\$.+?\$\$|\\\[.+?\\\]|\\\(.+?\\\)|(?<!\\)\$[^$\n]+?\$""")
    private val heading = Regex("""^\s{0,3}#{1,6}\s+(.+)$""")
    private val bullet = Regex("""^(\s*)[-*+]\s+(.+)$""")
    private val numbered = Regex("""^(\s*)(\d{1,2}[.)])\s+(.+)$""")
    private val bold = Regex("""\*\*(.+?)\*\*|__(.+?)__""")
    private val code = Regex("""`([^`\n]+)`""")
    private val italic = Regex("""(?<!\*)\*([^*\n]+)\*(?!\*)""")
    private val token = Regex("""\uE000(\d+)\uE001""")

    fun toHtml(source: String): String {
        val protected = mutableListOf<String>()
        val stable = math.replace(source.replace("\r\n", "\n").replace('\r', '\n')) {
            "\uE000${protected.also { list -> list.add(it.value) }.lastIndex}\uE001"
        }
        val out = StringBuilder()
        var fenced = false
        stable.lines().forEach { line ->
            if (line.trimStart().startsWith("```")) {
                out.append(if (fenced) "</code></pre>" else "<pre><code>")
                fenced = !fenced
                return@forEach
            }
            if (fenced) {
                out.append(escape(line)).append('\n')
                return@forEach
            }
            if (line.isBlank()) return@forEach
            val headingMatch = heading.matchEntire(line)
            val bulletMatch = bullet.matchEntire(line)
            val numberMatch = numbered.matchEntire(line)
            when {
                headingMatch != null -> out.append("<h3>").append(inline(headingMatch.groupValues[1])).append("</h3>")
                bulletMatch != null -> out.append("<p class='item' style='margin-left:")
                    .append((bulletMatch.groupValues[1].length * 6).coerceAtMost(36))
                    .append("px'>• ").append(inline(bulletMatch.groupValues[2])).append("</p>")
                numberMatch != null -> out.append("<p class='item' style='margin-left:")
                    .append((numberMatch.groupValues[1].length * 6).coerceAtMost(36))
                    .append("px'>").append(numberMatch.groupValues[2]).append(' ')
                    .append(inline(numberMatch.groupValues[3])).append("</p>")
                else -> out.append("<p>").append(inline(line.trim())).append("</p>")
            }
        }
        if (fenced) out.append("</code></pre>")
        return token.replace(out.toString()) { match ->
            protected.getOrNull(match.groupValues[1].toIntOrNull() ?: -1)?.let(::escape).orEmpty()
        }
    }

    private fun inline(value: String): String {
        var html = escape(value)
        html = code.replace(html) { "<code>${it.groupValues[1]}</code>" }
        html = bold.replace(html) { "<strong>${it.groupValues[1].ifEmpty { it.groupValues[2] }}</strong>" }
        html = italic.replace(html) { "<em>${it.groupValues[1]}</em>" }
        return html
    }

    private fun escape(value: String) = value.replace("&", "&amp;").replace("<", "&lt;")
        .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;")
}
