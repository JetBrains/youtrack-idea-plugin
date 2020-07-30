package com.github.jk1.ytplugin.issues.model

import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.intellij.tasks.Comment
import java.util.*


class IssueComment(item: JsonElement) {

    val id: String = item.asJsonObject.get("id").asString
    val authorLogin: String = item.asJsonObject.get("author").asJsonObject.get("login").asString
    val authorName: String = item.asJsonObject.get("author").asJsonObject.get("name").asString
    val text: String = item.asJsonObject.get("text").asString
    val created: Date = Date(item.asJsonObject.get("created").asLong)
    val updated: Date? = when (item.asJsonObject.get("updated")) {
        is JsonNull -> null
        else -> Date(item.asJsonObject.get("updated").asLong)
    }

    fun asTaskManagerComment() = object : Comment() {
        override fun getText() = this@IssueComment.text
        override fun getDate() = this@IssueComment.created
        override fun getAuthor() = this@IssueComment.authorName
    }
}