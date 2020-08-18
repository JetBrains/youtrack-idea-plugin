package com.github.jk1.ytplugin.rest

import com.github.jk1.ytplugin.issues.model.Issue
import com.github.jk1.ytplugin.issues.model.IssueWorkItem
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.google.gson.JsonArray
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
        val fields = NameValuePair("fields", "id,idReadable,updated,created," +
                "tags(color(foreground,background),name),project,links(value,direction,issues(idReadable)," +
                "linkType(name,sourceToTarget,targetToSource),id),comments(id,textPreview,created,updated," +
                "author(name,%20authorFullName,login)),summary,wikifiedDescription,customFields(name,value(name)," +
                "id,projectCustomField),resolved,attachments(name,url),description,reporter(login)")
        method.setQueryString(arrayOf(fields))
        val issue = method.execute { IssueJsonParser.parseIssue(it, repository.url) }

        return if (issue != null)
            getWorkItemsForIssues(issue.issueId, mutableListOf(issue))[0]
        else
            null
    }

    private fun parseIssues(method: GetMethod): MutableList<Issue> {
        return method.connect {
            val list: MutableList<Issue> = mutableListOf()
            val status = httpClient.executeMethod(method)
            val json: JsonArray = JsonParser.parseString(method.responseBodyAsString) as JsonArray

            json.map { list.add(IssueParser().parseIssue(it.asJsonObject, repository.url)) }

            if (status == 200) {
                list
            } else {
                throw RuntimeException(method.responseBodyAsLoggedString())
            }
        }
    }

    private fun parseWorkItems(method: GetMethod, issues: MutableList<Issue>): MutableList<Issue> {

        return method.connect { it ->
            val status = httpClient.executeMethod(method)
            val json: JsonArray = JsonParser.parseString(method.responseBodyAsString) as JsonArray
            val workItems = mutableListOf<IssueWorkItem>()
            json.mapNotNull { workItems.add(IssueJsonParser.parseWorkItem(it)!!) }

            for (item in workItems) {
                val issue: MutableList<Issue> = issues.filter { it.id == item.issueId }.toMutableList()
                issue[0].workItems.add(item)
            }

            if (status == 200) {
                issues
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
        val myFields = NameValuePair("fields", "id,idReadable,updated,created," +
                "tags(color(foreground,background),name),project,links(value,direction,issues(idReadable)," +
                "linkType(name,sourceToTarget,targetToSource),id),comments(id,textPreview,created,updated," +
                "author(name,%20authorFullName,login)),summary,wikifiedDescription,customFields(name,color,value(name,minutes,presentation,color(background,foreground)),id,projectCustomField)," +
                "resolved,attachments(name,url),description,reporter(login)")

        method.setQueryString(arrayOf(myQuery, myFields))
        val issues = parseIssues(method)

        return getWorkItemsForIssues(query, issues)
    }


    private fun getWorkItemsForIssues(query: String, issues: MutableList<Issue>): List<Issue> {
        val myQuery = NameValuePair("query", query)
        val url = "${repository.url}/api/workItems"
        val method = GetMethod(url)
        val myFields = NameValuePair("fields", "text,issue(idReadable),created," +
                "duration(presentation,minutes),author(name),creator(name),date,id")
        method.setQueryString(arrayOf(myQuery, myFields))

        return parseWorkItems(method, issues)
    }
}