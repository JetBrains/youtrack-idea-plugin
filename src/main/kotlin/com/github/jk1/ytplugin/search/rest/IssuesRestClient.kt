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

class IssuesRestClient(override val project: Project, val repo: YouTrackServer) : RestClientTrait, ResponseLoggerTrait {

    fun getIssues(query: String = ""): List<Issue> {
        val projects = getIssueIds(query).map { it.split("-")[0] }.distinct()
        return projects.flatMap { getIssuesInProject(it, query) }
    }

    fun getIssueIds(query: String = ""): List<String> {
        val method = GetMethod("${repo.url}/rest/issue?filter=${query.urlencoded}&max=30")
        return method.execute {
            val issues = it.asJsonObject.getAsJsonArray("issue")
            issues.map { it.asJsonObject.get("id").asString }
        }
    }

    fun getIssuesInProject(projectShortName: String, query: String = ""): List<Issue> {
        val url = "${repo.url}/rest/issue/byproject/${projectShortName.urlencoded}"
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