package com.example.asm_final.chat

import org.junit.Assert.*
import org.junit.Test

class ChatAnswerMarkupTest {
    @Test fun formatsNewtonAnswerWithoutLeakingMarkdownMarkers() {
        val html = ChatAnswerMarkup.toHtml(
            "### 2. Định luật II Newton\n**Nội dung:** Gia tốc (\$a\$) cùng hướng với lực.\n" +
                "* **Công thức:** \$F = m \\cdot a\$\n* Ví dụ: Xe đẩy hàng."
        )
        assertTrue(html.contains("<h3>2. Định luật II Newton</h3>"))
        assertTrue(html.contains("<strong>Nội dung:</strong>"))
        assertTrue(html.contains("• <strong>Công thức:</strong>"))
        assertTrue(html.contains("$" + "F = m \\cdot a" + "$"))
        assertFalse(html.contains("###"))
    }

    @Test fun escapesUntrustedHtmlAndKeepsVietnameseAndMath() {
        val html = ChatAnswerMarkup.toHtml("**Hóa học:** H₂O + \\(\\frac{1}{2}O_2\\) <script>alert(1)</script>")
        assertTrue(html.contains("H₂O"))
        assertTrue(html.contains("\\(\\frac{1}{2}O_2\\)"))
        assertTrue(html.contains("&lt;script&gt;"))
        assertFalse(html.contains("<script>"))
    }

    @Test fun preservesCodeWithoutExecutingMarkup() {
        val html = ChatAnswerMarkup.toHtml("```\n<unsafe>\n```\n`x < 2`")
        assertTrue(html.contains("<pre><code>&lt;unsafe&gt;"))
        assertTrue(html.contains("<code>x &lt; 2</code>"))
    }
}
