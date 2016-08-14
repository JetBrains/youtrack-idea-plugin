package com.github.jk1.ytplugin.search

import com.github.jk1.ytplugin.search.model.Issue
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import org.junit.Assert.*
import org.junit.Test
import java.io.InputStreamReader


class IssueJsonParseTest {

    val serverUrl = "http://youtrack.com"

    @Test
    fun testParseYouTrack52Issue() {
        val issue = Issue(getJsonElement("youtrack_5.2_issue.json"), serverUrl)

        assertEquals("D-7596", issue.id)
        assertEquals("summary", issue.summary)
        assertEquals("description", issue.description)
        assertNull(issue.entityId)
    }

    @Test
    fun testMalformedCustomField() {
        val issue = Issue(getJsonElement("malformed_custom_field.json"), serverUrl)

        assertEquals("JT-1", issue.id)
        assertEquals("25-4565", issue.entityId)
        assertEquals("formatting problems in comments", issue.summary)
        assertEquals("check out the source chunks in this issue", issue.description)
    }

    @Test
    fun testIssueTask() {
        val task = Issue(getJsonElement("malformed_custom_field.json"), serverUrl).asTask()

        assertEquals("$serverUrl/issue/JT-1", task.issueUrl)
        assertEquals("JT-1", task.id)
        assertEquals("formatting problems in comments", task.summary)
        assertEquals("check out the source chunks in this issue", task.description)
        assertTrue(task.isIssue)
        assertTrue(task.isClosed)
    }

    private fun getJsonElement(resourceName: String): JsonElement {
        val reader = InputStreamReader(this.javaClass.getResourceAsStream(resourceName))
        return JsonParser().parse(reader)
    }
}