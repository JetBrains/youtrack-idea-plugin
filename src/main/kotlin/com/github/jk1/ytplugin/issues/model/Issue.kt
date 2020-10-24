package com.github.jk1.ytplugin.issues.model

import com.github.jk1.ytplugin.YouTrackIssue
import com.github.jk1.ytplugin.rest.IssueJsonParser
import com.google.gson.JsonElement
import java.util.*

class Issue(item: JsonElement, val repoUrl: String) : YouTrackIssue {
    var json: String
    var id: String
    val entityId: String
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
    var workItems: MutableList<IssueWorkItem> = mutableListOf()

    init {
        val root = item.asJsonObject
        json = item.toString()
        id = root.get("idReadable").asString

        entityId = root.get("id").asString

        summary = root.get("summary")?.asString ?: ""

        val descriptionElement = root.get("wikifiedDescription")
        description = if (descriptionElement == null || descriptionElement.isJsonNull) "" else descriptionElement.asString

        createDate = Date(root.get("created")?.asLong ?: 0)

        updateDate = Date(root.get("updated")?.asLong ?: 0)

        resolved = (!root.get("resolved").isJsonNull && root.get("resolved") != null)

        customFields = if (root.getAsJsonArray("customFields") != null && !root.getAsJsonArray(("customFields")).isJsonNull){
            root.getAsJsonArray("customFields").mapNotNull { IssueJsonParser.parseCustomField(it) }
        } else {
            // YouTrack 2018.X has no 'customFields' yet
            root.getAsJsonArray("fields").mapNotNull { IssueJsonParser.parseCustomField(it) }
        }

        comments = root.getAsJsonArray("comments").mapNotNull { IssueJsonParser.parseComment(it) }

        val wrapper = IssueLinkWrapper()
        val result: MutableList<IssueLink> = mutableListOf()
        val myLinks = root.getAsJsonArray("links")
        for (element in myLinks)
            result.addAll(wrapper.reformatIssues(element, repoUrl))

        links = result.filter { it.value != "" }

        tags = root.getAsJsonArray(("tags")).mapNotNull { IssueJsonParser.parseTag(it) }

        attachments = root.getAsJsonArray(("attachments")).mapNotNull { IssueJsonParser.parseAttachment(it, repoUrl) }

        url = "$repoUrl/issue/$id"
    }

    override fun getIssueId() = id

    override fun getIssueSummary() = summary

    override fun getIssueDescription() = description

    override fun getIssueFields() = customFields

    override fun toString() = "$id $summary" // Quick search in issue list relies on that

    override fun equals(other: Any?) = toString() == other?.toString()

    override fun hashCode(): Int = toString().hashCode()

}


