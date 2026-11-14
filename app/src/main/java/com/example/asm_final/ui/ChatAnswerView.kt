package com.example.asm_final.ui

import android.graphics.Color
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import com.example.asm_final.chat.ChatAnswerMarkup

/** Render Markdown and TeX inside one self-sizing WebView, with local KaTeX assets only. */
@Composable
internal fun ChatAnswerView(source: String) {
    val color = MaterialTheme.colorScheme.onSurface.toArgb()
    val density = LocalDensity.current
    var height by remember(source) { mutableStateOf(80.dp) }
    val html = remember(source, color) { chatPage(ChatAnswerMarkup.toHtml(source), color) }
    AndroidView(
        modifier = Modifier.fillMaxWidth().height(height),
        factory = { context ->
            WebView(context).apply {
                setBackgroundColor(Color.TRANSPARENT)
                settings.javaScriptEnabled = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.blockNetworkLoads = true
                settings.domStorageEnabled = false
                settings.javaScriptCanOpenWindowsAutomatically = false
                settings.setSupportMultipleWindows(false)
                settings.textZoom = (100 * context.resources.configuration.fontScale).toInt()
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                isNestedScrollingEnabled = false
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, url: String?) = true
                }
                addJavascriptInterface(object {
                    @JavascriptInterface fun resize(cssHeight: Int) {
                        post {
                            if (cssHeight in 1..20000) {
                                val measured = with(density) { (cssHeight * scale.coerceAtLeast(1f)).toDp() }
                                    .coerceAtLeast(32.dp)
                                if (measured != height) height = measured
                            }
                        }
                    }
                }, "AndroidChat")
            }
        },
        update = { web ->
            if (web.tag != html) {
                web.tag = html
                web.loadDataWithBaseURL("file:///android_asset/katex/", html, "text/html", "UTF-8", null)
            }
        },
        onRelease = { web -> web.removeJavascriptInterface("AndroidChat"); web.destroy() },
    )
}

private fun chatPage(content: String, textColor: Int): String {
    val hex = "#%06X".format(textColor and 0xFFFFFF)
    return """<!DOCTYPE html><html><head><meta charset="UTF-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<link rel="stylesheet" href="katex.min.css">
<style>html,body{margin:0;padding:0;background:transparent;overflow-x:hidden}
body{color:$hex;font:16px/1.55 Roboto,'Noto Sans',sans-serif;overflow-wrap:anywhere}
p{margin:0 0 10px}p:last-child{margin-bottom:0}h3{font-size:1.08em;line-height:1.4;margin:15px 0 7px;font-weight:700}
h3:first-child{margin-top:0}.item{padding-left:2px}strong{font-weight:700}
code{font-family:Roboto,'Noto Sans',sans-serif;background:rgba(128,128,128,.13);border-radius:4px;padding:1px 3px}
pre{white-space:pre-wrap;overflow-wrap:anywhere;background:rgba(128,128,128,.12);padding:8px;border-radius:8px}
.katex-display{overflow-x:auto;overflow-y:hidden;margin:10px 0}</style>
<script defer src="katex.min.js"></script><script defer src="contrib/auto-render.min.js"></script>
<script>function updateHeight(){AndroidChat.resize(Math.ceil(document.body.scrollHeight)+8)}
window.addEventListener('load',function(){
if(window.renderMathInElement)renderMathInElement(document.body,{delimiters:[
{left:'$$',right:'$$',display:true},{left:'\\[',right:'\\]',display:true},
{left:'\\(',right:'\\)',display:false},{left:'$',right:'$',display:false}],throwOnError:false,trust:false});
updateHeight();if(window.ResizeObserver)new ResizeObserver(updateHeight).observe(document.body);
if(document.fonts)document.fonts.ready.then(updateHeight);
});</script></head><body>$content</body></html>"""
}
