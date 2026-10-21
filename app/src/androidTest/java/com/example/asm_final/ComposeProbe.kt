package com.example.asm_final

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createEmptyComposeRule

/** Semantic assertions for existing Java integration tests during the UI migration. */
object ComposeProbe {
    @JvmStatic fun rule(): ComposeTestRule = createEmptyComposeRule()
    @JvmStatic fun text(rule: ComposeTestRule, value: String) {
        rule.waitUntil(25000) { rule.onAllNodesWithText(value).fetchSemanticsNodes().isNotEmpty() }
    }
    @JvmStatic fun tag(rule: ComposeTestRule, value: String) {
        rule.waitUntil(25000) { rule.onAllNodesWithTag(value).fetchSemanticsNodes().isNotEmpty() }
    }
    @JvmStatic fun click(rule: ComposeTestRule, value: String) {
        text(rule, value)
        val node = rule.onAllNodesWithText(value).onFirst()
        runCatching { node.performScrollTo() }
        node.performClick()
    }
    @JvmStatic fun listClick(rule: ComposeTestRule, list: String, tag: String) {
        rule.onNodeWithTag(list).performScrollToNode(hasTestTag(tag))
        rule.onNodeWithTag(tag).performClick()
    }
}
