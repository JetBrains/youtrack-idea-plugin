package com.github.jk1.ytplugin.notifications

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.*
import java.util.zip.GZIPInputStream

class YouTrackNotification(item: JsonElement, val repoUrl: String) {

    val id: String
    val recipient: String
    val content: String
    val metadata: String
    val issueId: String
    val summary: String
    val url: String

    init {
        val root = item.asJsonObject
        id = root.get("id").asString
        metadata = decode(root.get("metadata").asString)
        recipient = item.asJsonObject.get("recipient")?.asJsonObject?.get("login")?.asString ?: ""
        val metadataElement = JsonParser.parseString(metadata).asJsonObject
        val issueElement = metadataElement.get("issue").asJsonObject
        issueId = issueElement.get("id").asString
        summary = htmlEscape(issueElement.get("summary").asString, "UTF-8")
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


    private fun htmlEscape(input: String, encoding: String): String {
        val escaped = StringBuilder(input.length * 2)
        for (element in input) {
            val reference = convertToReference(element, encoding)
            if (reference != null) {
                escaped.append(reference)
            } else {
                escaped.append(element)
            }
        }
        return escaped.toString()
    }


    private fun convertToReference(character: Char, encoding: String): String? {
        if (encoding.startsWith("UTF-")) {
            when (character) {
                '<' -> return "&lt;"
                '>' -> return "&gt;"
                '"' -> return "&quot;"
                '&' -> return "&amp;"
                '\'' -> return "&#39;"
            }
        }
        return null
    }

    private fun prettifyContent(content: String): String {
        return content.replace("<p>$url</p>\n" ,"")
    }
}