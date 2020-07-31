package com.github.jk1.ytplugin.issues.model

import com.github.jk1.ytplugin.YouTrackIssue
import com.github.jk1.ytplugin.rest.IssueJsonParser
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
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
        println("id $id")

//            println( "link" + currentIssue.links[0])
//            println( "tag" + currentIssue.tags[0])
//            println("att " + currentIssue.attachments)
//            println( "url " + currentIssue.url)
//            println("wiki" + currentIssue.wikified)
//

        entityId = root.get("id")?.asString
        println("entityId $entityId")

        summary = root.get("summary")?.asString  ?: ""
        println("summary $summary")

        description = if (root.get("description")== null || root.get("description").isJsonNull) ""
            else root.get("description").asString

        println("desc $description")

        wikified = true
        println("wiki $wikified")

        createDate = Date(root.get("created")?.asLong ?: 0)
        println("crDate $createDate")

        updateDate = Date(root.get("updated")?.asLong ?: 0)
        println("upDate$updateDate")

        resolved = root.get("resolved") != null
        println("reso $resolved")

        customFields = root.getAsJsonArray("customFields")
                .filter { it.isCustomField() }
                .map { IssueJsonParser.parseCustomField(it) }
                .filter { it != null }
                .requireNoNulls()
        if (customFields.isNotEmpty())
            for (i in 0 until customFields.size)
                println( "cf " + customFields[i].name)

        comments = root.getAsJsonArray("comments")
                .map { IssueJsonParser.parseComment(it) }
                .filter { it != null }
                .requireNoNulls()
        if (comments.isNotEmpty())
            println( "comm " + comments[0].text + " " + comments[0].authorName)

//        links = root.getAsJsonArray("links")
//                .map { IssueJsonParser.parseLink(it, repoUrl) }
//                .filter { it != null }
//                .requireNoNulls()
//        if (links.size > 0)
//            println( "link" + links[0].role + " " + links[0].type + " " + links[0].value)

        tags = root.getAsJsonArray(("tags"))
                .map { IssueJsonParser.parseTag(it) }
                .filter { it != null }
                .requireNoNulls()
        if (tags.isNotEmpty())
            println( "tags" + tags[0].backgroundColor + tags[0].foregroundColor + " " + tags[0].text)

        attachments = root.getAsJsonArray(("attachments"))
                .map { IssueJsonParser.parseAttachment(it, repoUrl) }
                .filter { it != null }
                .requireNoNulls()
        if (attachments.isNotEmpty())
            for (i in 0 until attachments.size)
            println( "attachments " + attachments[i].url + " " + attachments[i].fileName)

        url = "$repoUrl/issue/$id"
        println("url " + url)
    }

    override fun getIssueId() = id

    override fun getIssueSummary() = summary

    override fun getIssueDescription() = description

    override fun getIssueFields() = customFields

    override fun toString() = "$id $summary" // Quick search in issue list relies on that

    override fun equals(other: Any?) = toString() == other?.toString()

    override fun hashCode(): Int = toString().hashCode()

    private fun JsonObject.getFieldValue(name: String): JsonElement? {
        return this.getAsJsonArray("field").firstOrNull {
            name == it.asJsonObject.get("name")?.asString
        }?.asJsonObject?.get("value")
    }

    private fun JsonElement.isCustomField(): Boolean {
        val name = asJsonObject.get("name")
        return name != null && !PREDEFINED_FIELDS.contains(name.asString)
    }
}


