package com.github.jk1.ytplugin.rest

import com.github.jk1.ytplugin.issues.model.Issue
import com.github.jk1.ytplugin.issues.model.IssueWorkItem
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.apache.commons.httpclient.NameValuePair
import org.apache.commons.httpclient.methods.GetMethod
import org.apache.commons.httpclient.methods.PostMethod
import org.apache.commons.httpclient.methods.StringRequestEntity
import java.io.InputStreamReader
import java.net.URL
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
        val res: URL? = this::class.java.classLoader.getResource("create_draft_body.json")

        val summaryFormatted = summary.replace("\n", "\\n").replace("\"", "\\\"")
        val jsonBody = res?.readText()?.replace("{description}", summaryFormatted)

        method.requestEntity = StringRequestEntity(jsonBody, "application/json", StandardCharsets.UTF_8.name())
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
        val status = httpClient.executeMethod(method)
        if (status == 200) {
            val issues: JsonArray = JsonParser.parseReader(method.responseBodyAsReader).asJsonArray
            val issuesWithWorkItems: List<JsonElement> = mapWorkItemsWithIssues(issues)
            val fullIssues = parseIssues(issuesWithWorkItems)
            return fullIssues[0]
        }
        else
            return null
    }

    private fun parseIssues(json: List<JsonElement>): List<Issue> {
        return json.map { IssueParser().parseIssue(it.asJsonObject, repository.url) }
    }

    private fun parseWorkItems(method: GetMethod): List<IssueWorkItem> {
        return method.connect {
            val status = httpClient.executeMethod(method)
            val json: JsonArray = JsonParser.parseReader(method.responseBodyAsReader) as JsonArray
            if (status == 200) {
                val workItems = mutableListOf<IssueWorkItem>()
                json.mapNotNull { workItems.add(IssueJsonParser.parseWorkItem(it)!!) }
                workItems
            } else {
                throw RuntimeException(method.responseBodyAsLoggedString())
            }
        }
    }


    override fun getIssues(query: String): List<Issue> {
        // todo: customizable "max" limit
        val url = "${repository.url}/api/issues"
        val method = GetMethod(url)
        val myQuery = NameValuePair("query", query)
        val myFields = NameValuePair("fields", ISSUE_FIELDS)
        var fullIssues = listOf<Issue>()
        method.setQueryString(arrayOf(myQuery, myFields))
        val status = httpClient.executeMethod(method)
        if (status == 200) {
            val issues: JsonArray = JsonParser.parseReader(method.responseBodyAsReader) as JsonArray
            val issuesWithWorkItems: List<JsonElement> = mapWorkItemsWithIssues(issues)
            fullIssues = parseIssues(issuesWithWorkItems)
        } else {
            throw RuntimeException(method.responseBodyAsLoggedString())
        }

        return fullIssues
    }


    fun getWorkItemsForIssues(query: String): List<IssueWorkItem> {
        val myQuery = NameValuePair("query", query)
        val url = "${repository.url}/api/workItems"
        val method = GetMethod(url)
        val myFields = NameValuePair("fields", "text,type(name),created,issue(idReadable)," +
                "duration(presentation,minutes),author(name),creator(name),date,id")
        method.setQueryString(arrayOf(myQuery, myFields))

        return parseWorkItems(method)
    }

    fun mapWorkItemsWithIssues(issues: JsonArray): List<JsonElement> {

        var newIssues = mutableListOf<JsonElement>()
        for (issue in issues) {
            val id = issue.asJsonObject.get("idReadable").asString
            val workItems = getWorkItemsForIssues(id)

            var workItemsJson: String
            if (workItems.isEmpty()){
                workItemsJson  = "\"workItems\":null"
            } else {
                workItemsJson  = "\"workItems\": ["
                for (x in 0..(workItems.size - 2)){
                    workItemsJson += "${workItems[x].json},"
                }
                workItemsJson += "${workItems.last().json}]"
            }

            val issueString = issue.toString().replace("\"project\":{\"\$type\":\"Project\"}", workItemsJson)
            newIssues.add(JsonParser.parseString(issueString) as JsonElement)

        }

        return newIssues
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
                "0"
            }
        }
    }

    fun getUniqueIssueIds(): List<NameValuePair> {
        val myQuery = NameValuePair("fields", "idReadable,summary")
        val url = "${repository.url}/api/issues"
        val method = GetMethod(url)
        method.setQueryString(arrayOf(myQuery))
        val result = mutableListOf<NameValuePair>()
        return method.connect {
            val status = httpClient.executeMethod(method)
            if (status == 200) {
                val json: JsonArray = JsonParser.parseString(method.responseBodyAsString) as JsonArray
                for (item in json) {
                    val pair = NameValuePair(item.asJsonObject.get("idReadable").asString,
                            item.asJsonObject.get("idReadable").asString + ": " + item.asJsonObject.get("summary").asString)
                    result.add(pair)
                }
                result
            } else {
                result
            }
        }
    }
}