package com.github.jk1.ytplugin.search.rest

import com.github.jk1.ytplugin.common.logger
import com.github.jk1.ytplugin.search.model.*
import com.google.gson.JsonElement

object IssueJsonParser {

    fun parseIssue(element: JsonElement, url: String) = parseSafe(element, { Issue(element, url) })

    fun parseCustomField(element: JsonElement) = parseSafe(element, { CustomField(element) })

    fun parseComment(element: JsonElement) = parseSafe(element, { IssueComment(element) })

    fun parseTag(element: JsonElement) = parseSafe(element, { IssueTag(element) })

    fun parseLink(element: JsonElement) = parseSafe(element, { IssueLink(element) })

    private fun <T> parseSafe(element: JsonElement, parser: () -> T): T? {
        try {
            return parser.invoke()
        } catch(e: Exception) {
            logger.warn("YouTrack issue parse error. Offending element: $element")
            logger.debug(e)
            return null
        }
    }
}