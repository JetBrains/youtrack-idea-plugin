package com.github.jk1.ytplugin.rest

import com.github.jk1.ytplugin.issues.model.Issue
import com.github.jk1.ytplugin.issues.model.IssueWorkItem
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.github.jk1.ytplugin.timeTracker.TrackerNotifier
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.apache.commons.httpclient.NameValuePair
import org.apache.commons.httpclient.methods.GetMethod
import org.apache.commons.httpclient.methods.PostMethod
import org.apache.commons.httpclient.methods.StringRequestEntity
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets


/**
 * Fetches YouTrack issues with issue description formatted from wiki into html on server side.
 */
class IssuesRestClient(override val repository: YouTrackServer) : IssuesRestClientBase, RestClientTrait {

    companion object {
        const val ISSUE_FIELDS = "id,idReadable,updated,created," +
                "tags(color(foreground,background),name),project,links(value,direction,issues(idReadable)," +
                "linkType(name,sourceToTarget,targetToSource),id),comments(id,textPreview,created,updated," +
                "author(name,login)),summary,wikifiedDescription,customFields(name,color,value(name,minutes,presentation,color(background,foreground))," +
                "id,projectCustomField(emptyFieldText)),resolved,attachments(name,url),reporter(login)"
    }

    override fun createDraft(summary: String): String {
        val method = PostMethod("${repository.url}/api/admin/users/me/drafts")
        method.requestEntity = StringRequestEntity(summary, "application/json", StandardCharsets.UTF_8.name())

        return method.connect {
            val status = httpClient.executeMethod(method)
            if (status == 200) {
                val stream = InputStreamReader(method.responseBodyAsLoggedStream(), "UTF-8")
                JsonParser.parseReader(stream).asJsonObject.get("id").asString
            } else {
                throw RuntimeException(method.responseBodyAsLoggedString())
            }
        }
    }

    override fun getIssue(id: String): Issue? {
        val method = GetMethod("${repository.url}/api/issues/$id")
        val fields = NameValuePair("fields", ISSUE_FIELDS)
        method.setQueryString(arrayOf(fields))
        val issue = method.execute { IssueJsonParser.parseIssue(it, repository.url) }
        return if (issue != null){
            val workItems = getWorkItems(issue.issueId)
            mapIssuesWithWorkItems(workItems, listOf(issue))[0]
        }
        else
            null
    }

    private fun parseIssues(method: GetMethod): List<Issue> {
        return method.connect {
            val status = httpClient.executeMethod(method)
            if (status == 200) {
                val json: JsonArray = JsonParser.parseString(method.responseBodyAsString) as JsonArray
                json.map { IssueParser().parseIssue(it.asJsonObject, repository.url) }
            } else {
                throw RuntimeException(method.responseBodyAsLoggedString())
            }
        }
    }

    private fun parseWorkItems(method: GetMethod): List<IssueWorkItem> {
        return method.connect {
            val status = httpClient.executeMethod(method)
            if (status == 200) {
                val json: JsonArray = JsonParser.parseString(method.responseBodyAsString) as JsonArray
                val workItems = mutableListOf<IssueWorkItem>()
                json.mapNotNull { workItems.add(IssueJsonParser.parseWorkItem(it)!!) }
                workItems
            } else {
                throw RuntimeException(method.responseBodyAsLoggedString())
            }
        }
    }

    private fun mapIssuesWithWorkItems(workItems: List<IssueWorkItem>, issues: List<Issue>): List<Issue> {
        for (item in workItems) {
            val issue: List<Issue> = issues.filter { it.id == item.issueId }.toList()
            issue[0].workItems.add(item)
        }
        return issues
    }

    override fun getIssues(query: String): List<Issue> {
        // todo: customizable "max" limit
        val url = "${repository.url}/api/issues"
        val method = GetMethod(url)
        val myQuery = NameValuePair("query", query)
        val myFields = NameValuePair("fields", ISSUE_FIELDS)
        method.setQueryString(arrayOf(myQuery, myFields))

        val issues = parseIssues(method)
        val workItems = getWorkItems(query)

        return mapIssuesWithWorkItems(workItems, issues)
    }


    private fun getWorkItems(query: String): List<IssueWorkItem> {
        val myQuery = NameValuePair("query", query)
        val url = "${repository.url}/api/workItems"
        val method = GetMethod(url)
        val myFields = NameValuePair("fields", "text,issue(idReadable),created," +
                "duration(presentation,minutes),author(name),creator(name),date,id")
        method.setQueryString(arrayOf(myQuery, myFields))

        return parseWorkItems(method)
    }

    fun getEntityIdByIssueId(issueId: String): String {
        val myQuery = NameValuePair("fields", "id")
        val url = "${repository.url}/api/issues/${issueId}"
        val method = GetMethod(url)
        method.setQueryString(arrayOf(myQuery))

        return method.connect {
            val status = httpClient.executeMethod(method)
            if (status == 200) {
                val json: JsonObject = JsonParser.parseString(method.responseBodyAsString) as JsonObject
                json.get("id").asString
            } else {
                TrackerNotifier.infoBox("Could not post time: not a YouTrack issue", "")
                "0"
            }
        }
    }

}