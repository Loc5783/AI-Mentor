package com.example.asm_final.ui

import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class ChatAnswerRenderDeviceTest {
    @get:Rule val compose = createComposeRule()

    @Test fun newtonFormulaIsTypesetInsideChatBubble() {
        compose.setContent {
            MaterialTheme { ChatAnswerView("### Định luật Newton\nCông thức: \$F = m \\cdot a\$") }
        }
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        var web: WebView? = null
        instrumentation.runOnMainSync {
            val activity = ActivityLifecycleMonitorRegistry.getInstance()
                .getActivitiesInStage(Stage.RESUMED).first()
            web = findWebView(activity.window.decorView)
        }
        assertTrue("Chat WebView was not created", web != null)
        var rendered = false
        for (attempt in 1..20) {
            val done = CountDownLatch(1)
            instrumentation.runOnMainSync {
                web!!.evaluateJavascript(
                    "document.querySelector('.katex') !== null && document.querySelector('h3') !== null"
                ) { result -> rendered = result == "true"; done.countDown() }
            }
            assertTrue("WebView did not answer", done.await(2, TimeUnit.SECONDS))
            if (rendered) break
            SystemClock.sleep(150)
        }
        assertTrue("KaTeX or Markdown heading did not render", rendered)
    }

    private fun findWebView(view: View): WebView? {
        if (view is WebView) return view
        if (view is ViewGroup) for (index in 0 until view.childCount) {
            findWebView(view.getChildAt(index))?.let { return it }
        }
        return null
    }
}
