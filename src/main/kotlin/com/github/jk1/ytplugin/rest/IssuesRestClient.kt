package com.github.jk1.ytplugin.rest

import com.github.jk1.ytplugin.issues.model.Issue
import com.github.jk1.ytplugin.issues.model.IssueWorkItem
import com.github.jk1.ytplugin.logger
import com.github.jk1.ytplugin.setup.getInstanceVersion
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.github.jk1.ytplugin.timeTracker.TrackerNotification
import com.google.gson.*
import com.intellij.notification.NotificationType
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.utils.URIBuilder
import java.net.URL

/**
 * Fetches YouTrack issues with issue description formatted from wiki into html on server side.
 */
class IssuesRestClient(override val repository: YouTrackServer) : IssuesRestClientBase, RestClientTrait {

    companion object {
        const val ISSUE_FIELDS = "id,idReadable,updated,created," +
                "tags(color(foreground,background),name),project(shortName),links(value,direction,issues(idReadable)," +
                "linkType(name,sourceToTarget,targetToSource),id),comments(id,textPreview,created,updated," +
                "author(name,login),deleted),summary,wikifiedDescription,customFields(name,color," +
                "value(name,minutes,presentation,markdownText,color(background,foreground))," +
                "id,projectCustomField(emptyFieldText)),resolved,attachments(name,url),reporter(login)"
    }

    override fun createDraft(summary: String): String? {
        val version = getInstanceVersion()
        // backwards compatibility for the deprecated endpoint
        val method = if (version != null && version > 2022.2) {
            HttpPost("${repository.url}/api/users/me/drafts")
        } else {
            HttpPost("${repository.url}/api/admin/users/me/drafts")
        }

        val res: URL? = this::class.java.classLoader.getResource("create_draft_body.json")
        val summaryFormatted = summary.replace("\n", "\\n").replace("\"", "\\\"")
        method.entity = res?.readText()?.replace("{description}", summaryFormatted)?.jsonEntity
        try {
            return method.execute { element ->
                val id = element.asJsonObject.get("id").asString
                logger.debug("Successfully created issue draft: $id")
                id
            }
        } catch (e: Exception){
            val trackerNote = TrackerNotification()
            trackerNote.notify("YouTrack server integration is not configured yet " +
                    "or the connection might be lost", NotificationType.WARNING)
            return ""
        }
    }

    override fun getIssue(id: String): Issue {
        val builder = URIBuilder("${repository.url}/api/issues/$id")
        builder.addParameter("fields", ISSUE_FIELDS)
        val method = HttpGet(builder.build())
        return method.execute { element ->
            val issuesWithWorkItems: List<JsonElement> = mapWorkItemsWithIssues(listOf(element))
            val fullIssues = parseIssues(issuesWithWorkItems)
            logger.debug("Successfully fetched issue: $id")
            fullIssues[0]
        }
    }

    private fun mapWorkItemsWithIssues(issues: Iterable<JsonElement>): List<JsonElement> {
        val newIssues = mutableListOf<JsonElement>()
        for (issue in issues) {
            val id = issue.asJsonObject.get("idReadable").asString
            val workItems = getWorkItems(id)
            var workItemsJson: String
            if (workItems.isEmpty()) {
                workItemsJson = "\"workItems\": []"
            } else {
                workItemsJson = "\"workItems\": ["
                for (x in 0..workItems.size - 2) {
                    workItemsJson += "${workItems[x].json},"
                }
                workItemsJson += "${workItems.last().json}]"
            }
            // add workItems to issue
            val issueString = issue.toString().substring(0, issue.toString().length - 1) + ",\n$workItemsJson\n}"
            newIssues.add(JsonParser.parseString(issueString) as JsonElement)
        }
        return newIssues
    }

    private fun parseIssues(json: List<JsonElement>): List<Issue> {
        return json.map { IssueParser().parseIssue(it.asJsonObject, repository.url) }
    }

    override fun getIssues(query: String): List<Issue> {
        // todo: customizable "max" limit
        val builder = URIBuilder("${repository.url}/api/issues")
        builder.addParameter("query", query)
                .addParameter("\$top", "100")
                .addParameter("fields", ISSUE_FIELDS)
        val method = HttpGet(builder.build())
        return method.execute { element ->
            val issues: JsonArray = element.asJsonArray
            val issuesWithWorkItems: List<JsonElement> = mapWorkItemsWithIssues(issues)
            parseIssues(issuesWithWorkItems)
        }
    }

    private fun getWorkItems(query: String): List<IssueWorkItem> {
        val builder = URIBuilder("${repository.url}/api/workItems")
        builder.addParameter("\$top", "100")
                .addParameter("query", query)
                .addParameter("fields", "text,type(name),created,issue(idReadable)," +
                        "duration(presentation,minutes),author(name),creator(name),date,id,attributes(name,id,value(name))")
                .addParameter("sort", "descending")
        return HttpGet(builder.build()).execute { element ->
            element.asJsonArray.mapNotNull { IssueJsonParser.parseWorkItem(it) }
        }
    }
}