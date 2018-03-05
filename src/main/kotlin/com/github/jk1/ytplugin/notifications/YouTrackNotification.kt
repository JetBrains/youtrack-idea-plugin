package com.github.jk1.ytplugin.notifications

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.*
import java.util.zip.GZIPInputStream

class YouTrackNotification(item: JsonElement, val repoUrl: String) {

    val content: String
    val metadata: String
    val id: String
    val summary: String
    val url: String

    init {
        val root = item.asJsonObject
        content = prettifyContent(decode(root.get("content").asString))
        metadata = decode(root.get("metadata").asString)
        val metadataElement = JsonParser().parse(metadata).asJsonObject
        val issueElement = metadataElement.get("issue").asJsonObject
        id = issueElement.get("id").asString
        summary = issueElement.get("summary").asString
        url = "$repoUrl/issues/$id"
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
        return content.lines().filterIndexed { index, _ -> index != 4  }.joinToString("\n")
    }
}