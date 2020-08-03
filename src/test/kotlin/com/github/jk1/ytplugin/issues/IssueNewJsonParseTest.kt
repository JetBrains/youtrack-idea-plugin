package com.github.jk1.ytplugin.issues

import com.github.jk1.ytplugin.issues.model.Issue
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.InputStreamReader


class IssueNewJsonParseTest {

    private val serverUrl = "http://youtrack.com"

    @Test
    fun testParseYouTrack52Issue() {
        val issue = Issue(getJsonElement("youtrack_5.2_new_issue.json"), serverUrl)

        assertEquals("TP-2", issue.id)
        assertEquals(4, issue.links.size)
        assertEquals("more tests to the god of tests", issue.description)
        assertEquals("2-3", issue.entityId)
    }

    @Test
    fun testMalformedCustomField() {
        val issue = Issue(getJsonElement("malformed_custom_field.json"), serverUrl)

        assertEquals("JT-1", issue.id)
        assertEquals("25-4565", issue.entityId)
        assertEquals("formatting problems in comments", issue.summary)
        assertEquals("check out the source chunks in this issue", issue.description)
    }

    private fun getJsonElement(resourceName: String): JsonElement {
        val reader = InputStreamReader(this.javaClass.getResourceAsStream(resourceName))
        return JsonParser().parse(reader)
    }
}