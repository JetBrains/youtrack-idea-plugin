package com.github.jk1.ytplugin.notifications

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.*
import java.util.zip.GZIPInputStream

class YouTrackNotification(item: JsonElement, val repoUrl: String) {

    val id: String

    val content: String
    val metadata: String
    val issueId: String
    val summary: String
    val url: String

    init {
        val root = item.asJsonObject
        id = root.get("id").asString
        metadata = decode(root.get("metadata").asString)
        val metadataElement = JsonParser.parseString(metadata).asJsonObject
        val issueElement = metadataElement.get("issue").asJsonObject
        issueId = issueElement.get("id").asString
        summary = issueElement.get("summary").asString
        url = "$repoUrl/issue/$issueId"
        content = prettifyContent(decode(root.get("content").asString))
    }

    private fun decode(content: String): String {
        val gzipInput = ByteArrayInputStream(Base64.getDecoder().decode(content))
        val out = ByteArrayOutputStream()
        GZIPInputStream(gzipInput).use {
            it.copyTo(out)
        }
        return out.toString("UTF-8")
    }

    private fun prettifyContent(content: String): String {
        return content.replace("<p>$url</p>\n" ,"")
    }
}