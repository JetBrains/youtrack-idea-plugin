package com.github.jk1.ytplugin.rest

import com.github.jk1.ytplugin.issues.model.Issue
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import org.apache.commons.httpclient.HttpMethod
import org.apache.commons.httpclient.methods.GetMethod
import java.io.InputStreamReader

/**
 * Fetches YouTrack issues with issue description formatted from wiki into html on server side.
 */
class IssuesRestClient(val repo: YouTrackServer) : RestClientTrait, ResponseLoggerTrait {

    override val project = repo.project

    fun getIssue(id: String): Issue? {
        val method = GetMethod("${repo.url}/rest/issue/$id?wikifyDescription=true")
        return method.execute { IssueJsonParser.parseIssue(it, repo.url) }
    }

    /**
     * There's no direct API to get formatted issues by a search query, so two-stage fetch is used:
     * - Fetch issues by search query and select all projects these issues belong to
     * - For each project request formatted issues with an auxiliary search request like
     * "issue id: PR-1 or issue id: PR-2 or ...". Auxiliary source request is necessary
     *  due to https://github.com/jk1/youtrack-idea-plugin/issues/30
     * - Sort issues to match the order from the first request
     */
    fun getIssues(query: String = ""): List<Issue> {
        val issuesIds = getIssueIds(query)
        val projects = issuesIds.groupBy { it.split("-")[0] }
        val wikifiedIssues = projects.flatMap {
            val issueIdsQuery = it.value.joinToString(prefix = "issue id: ", separator = ", ")
            getWikifiedIssuesInProject(it.key, issueIdsQuery)
        }
        return issuesIds.map { id -> wikifiedIssues.first { issue -> id == issue.id } }
    }

    private fun getIssueIds(query: String = ""): List<String> {
        val method = GetMethod("${repo.url}/rest/issue?filter=${query.urlencoded}&max=30&useImplicitSort=true")
        return method.execute {
            val issues = it.asJsonObject.getAsJsonArray("issue")
            issues.map { it.asJsonObject.get("id").asString }
        }
    }

    private fun getWikifiedIssuesInProject(projectShortName: String, query: String = ""): List<Issue> {
        val url = "${repo.url}/rest/issue/byproject/${projectShortName.urlencoded}"
        // todo: customizable "max" limit
        val params = "filter=${query.urlencoded}&wikifyDescription=true&max=30"
        val method = GetMethod("$url?$params")
        return method.execute { it.asJsonArray.map { IssueJsonParser.parseIssue(it, repo.url) }.filterNotNull() }
    }

    private fun <T> HttpMethod.execute(responseParser: (json: JsonElement) -> T): T {
        this.setRequestHeader("Accept", "application/json")
        return connect(this) {
            val status = createHttpClient(repo).executeMethod(this)
            if (status == 200) {
                val stream = InputStreamReader(this.responseBodyAsLoggedStream(), "UTF-8")
                responseParser.invoke(JsonParser().parse(stream))
            } else {
                throw RuntimeException(this.responseBodyAsLoggedString())
            }
        }
    }
}