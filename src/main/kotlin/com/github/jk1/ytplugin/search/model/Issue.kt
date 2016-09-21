package com.github.jk1.ytplugin.search.model

import com.github.jk1.ytplugin.search.rest.IssueJsonParser
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import java.util.*

class Issue(item: JsonElement, val repoUrl: String) {

    private val PREDEFINED_FIELDS = arrayOf("projectShortName", "numberInProject", "summary",
            "description", "created", "updated", "updaterName", "updaterFullName", "resolved",
            "reporterName", "reporterFullName", "commentsCount", "votes", "attachments", "links",
            "sprint", "voterName", "permittedGroup")

    val json: String

    val id: String
    val entityId: String?      // youtrack 5.2 doesn't expose entity id via rest
    val summary: String
    val description: String
    val createDate: Date
    val updateDate: Date
    val resolved: Boolean
    val customFields: List<CustomField>
    val comments: List<IssueComment>
    val links: List<IssueLink>
    val tags: List<IssueTag>
    val url: String

    init {
        val root = item.asJsonObject
        json = item.toString()
        id = root.get("id").asString
        entityId = root.get("entityId")?.asString
        summary = root.getFieldValue("summary")?.asString ?: ""
        description = root.getFieldValue("description")?.asString ?: ""
        createDate = Date(root.getFieldValue("created")?.asLong ?: 0)
        updateDate = Date(root.getFieldValue("updated")?.asLong ?: 0)
        resolved = root.getFieldValue("resolved") != null
        customFields = root.getAsJsonArray("field")
                .filter { it.isCustomField() }
                .map { IssueJsonParser.parseCustomField(it) }
                .filter { it != null }
                .requireNoNulls()
        comments = root.getAsJsonArray("comment")
                .map { IssueJsonParser.parseComment(it) }
                .filter { it != null }
                .requireNoNulls()
        links = (root.getFieldValue("links")?.asJsonArray ?: JsonArray())
                .map { IssueJsonParser.parseLink(it, repoUrl) }
                .filter { it != null }
                .requireNoNulls()
        tags = root.getAsJsonArray(("tag"))
                .map { IssueJsonParser.parseTag(it) }
                .filter { it != null }
                .requireNoNulls()
        url = "$repoUrl/issue/$id"
    }

    override fun toString() = "$id $summary" // Quick search in issue list relies on that

    private fun JsonObject.getFieldValue(name: String): JsonElement? {
        return this.getAsJsonArray("field").firstOrNull {
            name.equals(it.asJsonObject.get("name").asString)
        }?.asJsonObject?.get("value")
    }

    private fun JsonElement.isCustomField(): Boolean {
        val name = asJsonObject.get("name")
        return name != null && !PREDEFINED_FIELDS.contains(name.asString)
    }
}


