package com.github.jk1.ytplugin.rest

import com.github.jk1.ytplugin.issues.model.*
import com.github.jk1.ytplugin.logger
import com.google.gson.JsonElement

object IssueJsonParser {

    fun parseIssue(element: JsonElement, url: String) = parseSafe(element) { Issue(element, url) }

    fun parseWorkItem(element: JsonElement) = parseSafe(element) { IssueWorkItem(element) }

    fun parseWorkItemAttribute(element: JsonElement) = parseSafe(element) { WorkItemAttribute(element) }

    fun parseCustomField(element: JsonElement) = parseSafe(element) { CustomField(element) }

    fun parseComment(element: JsonElement) = parseSafe(element) { IssueComment(element) }

    fun parseTag(element: JsonElement) = parseSafe(element) { IssueTag(element) }

    fun parseAttachment(element: JsonElement, url: String) = parseSafe(element) { Attachment(element, url) }

    private fun <T> parseSafe(element: JsonElement, parser: () -> T): T? {
        return try {
            parser.invoke()
        } catch(e: Exception) {
            logger.warn("YouTrack issue parse error. Offending element: $element")
            logger.debug(e)
            null
        }
    }
}