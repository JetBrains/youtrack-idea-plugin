package com.github.jk1.ytplugin.rest
import com.github.jk1.ytplugin.issues.model.*
import com.github.jk1.ytplugin.rest.IssueJsonParser
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import java.util.*

class IssueParser(){

    fun parseIssue(item: JsonElement, repoUrl: String): Issue{
        println("i am in parser1")

        val issue = Issue(item, repoUrl)
        println("i am in parser2")

        val root = item.asJsonObject
        println("i am in parser3")

        issue.id = root.get("idReadable").asString
        println("i am in parser" + issue.id)

////        issue.entityId = root.get("id")?.asString
//        issue.summary = root.getFieldValue("summary")?.asString  ?: ""
//        issue.description = root.getFieldValue("description")?.asString ?: ""
//        issue.wikified = true
//        issue.createDate = Date(root.getFieldValue("created")?.asLong ?: 0)
//        issue.updateDate = Date(root.getFieldValue("updated")?.asLong ?: 0)
//        issue.resolved = root.getFieldValue("resolved") != null
//        issue.customFields = root.getAsJsonArray("customFields")
//                .filter { it.isCustomField() }
//                .map { IssueJsonParser.parseCustomField(it) }
//                .filter { it != null }
//                .requireNoNulls()
//        issue.comments = root.getAsJsonArray("comments")
//                .map { IssueJsonParser.parseComment(it) }
//                .filter { it != null }
//                .requireNoNulls()
//        issue.links = (root.getFieldValue("links")?.asJsonArray ?: JsonArray())
//                .map { IssueJsonParser.parseLink(it, repoUrl) }
//                .filter { it != null }
//                .requireNoNulls()
//        issue.tags = root.getAsJsonArray(("tags"))
//                .map { IssueJsonParser.parseTag(it) }
//                .filter { it != null }
//                .requireNoNulls()
//        issue.attachments = (root.getFieldValue("attachments")?.asJsonArray ?: JsonArray())
//                .map { IssueJsonParser.parseAttachment(it) }
//                .filter { it != null }
//                .requireNoNulls()
//        issue.url = "$repoUrl/issue/$issue.id"
        return issue
    }

    private fun JsonObject.getFieldValue(name: String): JsonElement? {
        return this.getAsJsonArray("field").firstOrNull {
            name == it.asJsonObject.get("name")?.asString
        }?.asJsonObject?.get("value")
    }

    private fun JsonElement.isCustomField(): Boolean {
        val name = asJsonObject.get("name")
        return name != null && !Issue.PREDEFINED_FIELDS.contains(name.asString)
    }
}


