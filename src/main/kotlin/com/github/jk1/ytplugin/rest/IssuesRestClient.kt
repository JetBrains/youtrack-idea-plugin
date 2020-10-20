package com.github.jk1.ytplugin.rest

import com.github.jk1.ytplugin.issues.model.Issue
import com.github.jk1.ytplugin.issues.model.IssueWorkItem
import com.github.jk1.ytplugin.logger
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.google.gson.JsonArray
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
                "fields(id,name,projectCustomField(emptyFieldText,field(name)),value(color(background,foreground),minutes,name,presentation))," +
                "tags(color(foreground,background),name),project,links(value,direction,issues(idReadable)," +
                "linkType(name,sourceToTarget,targetToSource),id),comments(id,textPreview,created,updated," +
                "author(name,login),deleted),summary,wikifiedDescription,customFields(name,color," +
                "value(name,minutes,presentation,color(background,foreground))," +
                "id,projectCustomField(emptyFieldText)),resolved,attachments(name,url),reporter(login)"
    }

    override fun createDraft(summary: String): String {
        val method = PostMethod("${repository.url}/api/admin/users/me/drafts")
        val res: URL? = this::class.java.classLoader.getResource("create_draft_body.json")

        val summaryFormatted = summary.replace("\n","\\n").replace("\"","\\\"")
        val jsonBody = res?.readText()?.replace("{description}", summaryFormatted)

        method.requestEntity = StringRequestEntity(jsonBody, "application/json", StandardCharsets.UTF_8.name())
        return method.connect {
            val status = httpClient.executeMethod(method)
            if (status == 200) {
                logger.debug("Successfully created draft: status $status")
                val stream = InputStreamReader(method.responseBodyAsLoggedStream(), "UTF-8")
                JsonParser.parseReader(stream).asJsonObject.get("id").asString
            } else {
                logger.error("Failed to create draft")
                throw RuntimeException(method.responseBodyAsLoggedString())
            }
        }
    }

    override fun getIssue(id: String): Issue? {
        val method = GetMethod("${repository.url}/api/issues/$id")
        val fields = NameValuePair("fields", ISSUE_FIELDS)
        method.setQueryString(arrayOf(fields))
        val issue = method.execute { IssueJsonParser.parseIssue(it, repository.url) }
        return if (issue != null) {
            logger.debug("Successfully fetched issue")
            getWorkItemsForIssues(issue.issueId, mutableListOf(issue))[0]
        }
        else{
            logger.warn("Failed to fetch issue with id $id: ${method.responseBodyAsLoggedString()}")
            null
        }
    }

    private fun parseWorkItems(method: GetMethod, issues: List<Issue>): List<Issue> {
        return method.connect {
            val status = httpClient.executeMethod(method)
            val json: JsonArray = JsonParser.parseString(method.responseBodyAsString) as JsonArray
            if (status == 200) {
                logger.debug("Successfully parsed work items: $status")
                val workItems = mutableListOf<IssueWorkItem>()
                json.mapNotNull { workItems.add(IssueJsonParser.parseWorkItem(it)!!) }
                for (item in workItems) {
                    val issue: MutableList<Issue> = issues.filter { it.id == item.issueId }.toMutableList()
                    if (issue.isNotEmpty()){
                        issue[0].workItems.add(item)
                    } else {
                        logger.debug("Unable to find issue corresponding to work item ${item.id}")
                    }
                }
                issues
            } else {
                logger.error("Unable to parse work items")
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
        method.setQueryString(arrayOf(myQuery, myFields))
        val issues = method.connect {
            val status = httpClient.executeMethod(method)
            if (status == 200) {
                logger.debug("Successfully fetched issues : $status")
                val json: JsonArray = JsonParser.parseReader(method.responseBodyAsReader) as JsonArray
                json.map { IssueParser().parseIssue(it.asJsonObject, repository.url) }
            } else {
                logger.error("Failed to fetch issues for query $query, issues: {}: ${method.responseBodyAsLoggedString()}")
                throw RuntimeException(method.responseBodyAsLoggedString())
            }
        }

        return getWorkItemsForIssues(query, issues)
    }


    private fun getWorkItemsForIssues(query: String, issues: List<Issue>): List<Issue> {
        val myQuery = NameValuePair("query", query)
        val url = "${repository.url}/api/workItems"
        val method = GetMethod(url)
        val myFields = NameValuePair("fields", "text,issue(idReadable)," +
                "duration(presentation,minutes),author(name),creator(name),date,id")
        method.setQueryString(arrayOf(myQuery, myFields))

        return parseWorkItems(method, issues)
    }
}