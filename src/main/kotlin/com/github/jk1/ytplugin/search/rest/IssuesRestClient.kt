package com.github.jk1.ytplugin.search.rest

import com.github.jk1.ytplugin.common.YouTrackServer
import com.github.jk1.ytplugin.common.rest.ResponseLoggerTrait
import com.github.jk1.ytplugin.common.rest.RestClientTrait
import com.github.jk1.ytplugin.search.model.Issue
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.intellij.openapi.project.Project
import org.apache.commons.httpclient.HttpMethod
import org.apache.commons.httpclient.methods.GetMethod
import java.io.InputStreamReader

/**
 * Fetches YouTrack issues with issue description formatted from wiki into html on server side.
 * There's no direct API to get formatted issues by a search query, so two-stage fetch is used:
 * - Fetch issues by search query and select all projects these issues belong to
 * - For each project request formatted issues with the same search query applied
 */
class IssuesRestClient(override val project: Project, val repo: YouTrackServer) : RestClientTrait, ResponseLoggerTrait {

    fun getIssues(query: String = ""): List<Issue> {
        val projects = getIssueIds(query).map { it.split("-")[0] }.distinct()
        return projects.flatMap { getWikifiedIssuesInProject(it, query) }
    }

    private fun getIssueIds(query: String = ""): List<String> {
        val method = GetMethod("${repo.url}/rest/issue?filter=${query.urlencoded}&max=30")
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
        return method.execute { it.asJsonArray.map { Issue(it, repo.url) } }
    }

    private fun <T> HttpMethod.execute(responseParser: (json: JsonElement) -> T): T {
        this.setRequestHeader("Accept", "application/json")
        try {
            val status = createHttpClient(repo).executeMethod(this)
            if (status == 200) {
                val stream = InputStreamReader(this.responseBodyAsLoggedStream())
                val root = JsonParser().parse(stream)
                return responseParser.invoke(root)
            } else {
                throw RuntimeException(this.responseBodyAsLoggedString())
            }
        } finally {
            this.releaseConnection()
        }
    }
}