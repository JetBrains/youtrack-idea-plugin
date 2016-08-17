package com.github.jk1.ytplugin.search.model

import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.intellij.tasks.Comment
import java.util.*


class IssueComment(item: JsonElement) {

    val id: String
    val authorLogin: String
    val authorName: String
    val text: String
    val permittedGroup: String?
    val created: Date
    val updated: Date?

    init {
        id = item.asJsonObject.get("id").asString
        authorLogin = item.asJsonObject.get("author").asString
        authorName = item.asJsonObject.get("authorFullName").asString
        text = item.asJsonObject.get("text").asString
        created = Date(item.asJsonObject.get("created").asLong)
        updated = when (item.asJsonObject.get("updated")) {
            is JsonNull -> null
            else -> Date(item.asJsonObject.get("updated").asLong)
        }
        permittedGroup = when (item.asJsonObject.get("permittedGroup")){
            is JsonNull -> null
            else -> item.asJsonObject.get("permittedGroup").asString
        }
    }

    fun asTaskManagerComment() = object : Comment() {
        override fun getText() = this@IssueComment.text
        override fun getDate() = this@IssueComment.created
        override fun getAuthor() = this@IssueComment.authorName
    }
}