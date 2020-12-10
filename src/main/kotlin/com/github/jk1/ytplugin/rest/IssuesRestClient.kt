package com.github.jk1.ytplugin.rest

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.issues.PersistentIssueStore
import com.github.jk1.ytplugin.issues.model.Issue
import com.github.jk1.ytplugin.issues.model.IssueWorkItem
import com.github.jk1.ytplugin.logger
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.google.gson.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import org.apache.commons.httpclient.HttpClient
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
        const val SUMMARY_LENGTH_MAX = 38
        const val ISSUE_FIELDS = "id,idReadable,updated,created," +
                "tags(color(foreground,background),name),project(shortName),links(value,direction,issues(idReadable)," +
                "linkType(name,sourceToTarget,targetToSource),id),comments(id,textPreview,created,updated," +
                "author(name,login),deleted),summary,wikifiedDescription,customFields(name,color," +
                "value(name,minutes,presentation,color(background,foreground))," +
                "id,projectCustomField(emptyFieldText)),resolved,attachments(name,url),reporter(login)"


        fun getEntityIdByIssueId(issueId: String, project: Project): String {
            val task = ComponentAware.of(project).taskManagerComponent.getTaskManager().activeTask
            if (!task.isIssue) {
                logger.debug("No valid YouTrack active task selected, ${task.id} is selected")
                return "0"
            }
            val repo = ComponentAware.of(project).taskManagerComponent.getActiveYouTrackRepository()
            val myQuery = NameValuePair("fields", "id")
            val url = "${repo.url}/api/issues/${issueId}"

            val client = HttpClient()
            val method = GetMethod(url)
            method.setQueryString(arrayOf(myQuery))
            method.setRequestHeader("Authorization", "Bearer " + repo.password)

            try {
                val status = client.executeMethod(method)
                return if (status == 200) {
                    logger.debug("Successfully found entity Id by issue Id: status $status")
                    val json: JsonObject = JsonParser.parseString(method.responseBodyAsString) as JsonObject
                    json.get("id").asString
                } else {
                    logger.debug("Failed to find entity Id by issue Id: code $status, ${method.responseBodyAsString}")
                    "0"
                }
            } catch (e: Exception) {
                logger.debug("Failed to get entity Id by issue Id: ${e.message}")
            }
            return "0"
        }
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
                logger.debug("Successfully created draft: status $status")
                val stream = InputStreamReader(method.responseBodyAsLoggedStream(), "UTF-8")
                JsonParser.parseReader(stream).asJsonObject.get("id").asString
            } else {
                logger.debug("Failed to create draft: ${method.responseBodyAsLoggedString()}")
                throw RuntimeException(method.responseBodyAsLoggedString())
            }
        }
    }

    override fun getIssue(id: String): Issue? {
        val method = GetMethod("${repository.url}/api/issues/$id")
        val fields = NameValuePair("fields", ISSUE_FIELDS)
        method.setQueryString(arrayOf(fields))
        val status = httpClient.executeMethod(method)
        return if (status == 200) {
            logger.debug("Successfully fetched issue: status $status")
            val issues: JsonArray = JsonParser.parseReader(method.responseBodyAsReader).asJsonArray
            val issuesWithWorkItems: List<JsonElement> = mapWorkItemsWithIssues(issues)
            val fullIssues = parseIssues(issuesWithWorkItems)
            fullIssues[0]
        } else {
            logger.debug("Failed to fetch issue with id $id: ${method.responseBodyAsLoggedString()}")
            null
        }
    }

    private fun parseIssues(json: List<JsonElement>): List<Issue> {
        return json.map { IssueParser().parseIssue(it.asJsonObject, repository.url) }
    }

    private fun parseWorkItems(method: GetMethod): List<IssueWorkItem> {
        return method.connect {
            val workItems = mutableListOf<IssueWorkItem>()
            val status = httpClient.executeMethod(method)
            val json: JsonArray = JsonParser.parseReader(method.responseBodyAsReader) as JsonArray
            if (status == 200) {
                logger.debug("Successfully parsed work items: $status")
                json.mapNotNull { workItems.add(IssueJsonParser.parseWorkItem(it)!!) }
                workItems
            } else {
                throw RuntimeException("Unable to parse work items: ${method.responseBodyAsLoggedString()}")
            }
        }
    }


    override fun getIssues(query: String): List<Issue> {
        // todo: customizable "max" limit
        val url = "${repository.url}/api/issues"
        val method = GetMethod(url)
        val myQuery = NameValuePair("query", query)
        val myTop = NameValuePair("\$top", "100")   // todo: customizable "max" limit
        val myFields = NameValuePair("fields", ISSUE_FIELDS)
        var fullIssues: List<Issue>
        method.setQueryString(arrayOf(myQuery, myFields, myTop))

        return method.connect {
            val status = httpClient.executeMethod(method)
            if (status == 200) {
                logger.debug("Successfully fetched issues : $status")
                val issues: JsonArray = JsonParser.parseReader(method.responseBodyAsReader) as JsonArray
                val issuesWithWorkItems: List<JsonElement> = mapWorkItemsWithIssues(issues)
                fullIssues = parseIssues(issuesWithWorkItems)
                fullIssues
            } else {
                logger.debug("Failed to fetch issues for query $query, issues: {}: ${method.responseBodyAsLoggedString()}")
                throw RuntimeException(method.responseBodyAsLoggedString())
            }
        }

    }

    private fun getWorkItems(query: String): List<IssueWorkItem> {
        val myQuery = NameValuePair("query", query)
        val url = "${repository.url}/api/workItems"
        val method = GetMethod(url)
        val myFields = NameValuePair("fields", "text,type(name),created,issue(idReadable)," +
                "duration(presentation,minutes),author(name),creator(name),date,id")
        val myTop = NameValuePair("\$top", "100")
        method.setQueryString(arrayOf(myQuery, myFields, myTop))

        return parseWorkItems(method)
    }

    private fun mapWorkItemsWithIssues(issues: JsonArray): List<JsonElement> {
        val newIssues = mutableListOf<JsonElement>()
        for (issue in issues) {
            val id = issue.asJsonObject.get("idReadable").asString
            val workItems = getWorkItems(id)

            var workItemsJson: String
            if (workItems.isEmpty()) {
                workItemsJson = "\"workItems\": []"
            } else {
                workItemsJson = "\"workItems\": ["
                for (x in 0..(workItems.size - 2)) {
                    workItemsJson += "${workItems[x].json},"
                }
                workItemsJson += "${workItems.last().json}]"
            }
            // project field is not going to be used, so could be replaced with valuable workItems
            val issueString = issue.toString().replace("\"project\":{\"\$type\":\"Project\"}", workItemsJson)
            newIssues.add(JsonParser.parseString(issueString) as JsonElement)
        }
        return newIssues
    }

    fun getIssueIdsAvailableForTracking(): List<NameValuePair> {

        val result = mutableListOf<NameValuePair>()
        val issueService = (ApplicationManager.getApplication().getService(PersistentIssueStore::class.java)!!)[repository]
        val issues = issueService.getAllIssues()
        val availableForTrackingProjects = AdminRestClient(repository).getTimeTrackingOptionsForProjects()

        for (issue in issues) {
            if (availableForTrackingProjects.find { it == issue.projectName } != null) {
                val pair = NameValuePair(issue.issueId, "${issue.issueId}: ${issue.summary}")
                result.add(pair)
            }
        }
        return result
    }
}