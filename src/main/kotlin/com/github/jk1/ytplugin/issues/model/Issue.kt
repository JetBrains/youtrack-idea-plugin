package com.github.jk1.ytplugin.issues.model

import com.github.jk1.ytplugin.YouTrackIssue
import com.github.jk1.ytplugin.rest.IssueJsonParser
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import org.apache.commons.lang3.mutable.Mutable
import java.util.*

class Issue(item: JsonElement, val repoUrl: String): YouTrackIssue {

    companion object {
        val PREDEFINED_FIELDS = arrayOf("projectShortName", "numberInProject", "summary",
                "description", "created", "updated", "updaterName", "updaterFullName", "resolved",
                "reporterName", "reporterFullName", "commentsCount", "votes", "attachments", "links",
                "sprint", "voterName", "permittedGroup", "markdown", "wikified")
    }

    var json: String
    var id: String
    val entityId: String?      // youtrack 5.2 doesn't expose entity id via rest
    var summary: String
    var description: String = "none"
    var createDate: Date
    var updateDate: Date
    var resolved: Boolean
    var customFields: List<CustomField> = emptyList()
    var comments: List<IssueComment> = emptyList()
    var links: List<IssueLink> = emptyList()
    var tags: List<IssueTag> = emptyList()
    var attachments: List<Attachment> = emptyList()
    var url: String
    var wikified: Boolean

    init {
        val root = item.asJsonObject
        json = item.toString()
        id = root.get("idReadable").asString

        entityId = root.get("id")?.asString

        summary = root.get("summary")?.asString  ?: ""

        description = if (root.get("description")== null || root.get("description").isJsonNull) ""
            else root.get("description").asString

        wikified = true

        createDate = Date(root.get("created")?.asLong ?: 0)

        updateDate = Date(root.get("updated")?.asLong ?: 0)

        resolved = root.get("resolved") != null

        customFields = root.getAsJsonArray("customFields")
                .filter { it.isCustomField() }
                .map { IssueJsonParser.parseCustomField(it) }
                .filter { it != null }
                .requireNoNulls()

        comments = root.getAsJsonArray("comments")
                .map { IssueJsonParser.parseComment(it) }
                .filter { it != null }
                .requireNoNulls()

        val wrapper =  IssueLinkWrapper()
        val result: MutableList<IssueLink> = mutableListOf()
        val myLinks = root.getAsJsonArray("links")
        for (element in myLinks)
            result.addAll(wrapper.reformatIssues(element, repoUrl))

        links = result.filter { it.value != "" }
                .requireNoNulls()

        tags = root.getAsJsonArray(("tags"))
                .map { IssueJsonParser.parseTag(it) }
                .filter { it != null }
                .requireNoNulls()

        attachments = root.getAsJsonArray(("attachments"))
                .map { IssueJsonParser.parseAttachment(it, repoUrl) }
                .filter { it != null }
                .requireNoNulls()

        url = "$repoUrl/issue/$id"
    }

    override fun getIssueId() = id

    override fun getIssueSummary() = summary

    override fun getIssueDescription() = description

    override fun getIssueFields() = customFields

    override fun toString() = "$id $summary" // Quick search in issue list relies on that

    override fun equals(other: Any?) = toString() == other?.toString()

    override fun hashCode(): Int = toString().hashCode()

    private fun JsonElement.isCustomField(): Boolean {
        val name = asJsonObject.get("name")
        return name != null && !PREDEFINED_FIELDS.contains(name.asString)
    }
}


