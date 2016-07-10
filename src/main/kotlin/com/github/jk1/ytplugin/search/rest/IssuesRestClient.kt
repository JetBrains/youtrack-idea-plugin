package com.github.jk1.ytplugin.search.rest

import com.github.jk1.ytplugin.common.rest.ResponseLoggerTrait
import com.github.jk1.ytplugin.common.rest.RestClientTrait
import com.github.jk1.ytplugin.search.model.Issue
import com.google.gson.JsonParser
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import org.apache.commons.httpclient.methods.GetMethod
import java.io.InputStreamReader

class IssuesRestClient(override val project: Project) : RestClientTrait, ResponseLoggerTrait {

    fun getIssues(indicator: ProgressIndicator, stamp: Long): List<Issue> {
        val baseUrl = taskManagerComponent.getActiveYouTrackRepository().url
        val getUsersUrl = "$baseUrl/rest/issues"
        val method = GetMethod(getUsersUrl)

        try {
            val status = createHttpClient().executeMethod(method)
            if (status == 200) {
                val stream = InputStreamReader(method.responseBodyAsLoggedStream())
                val root = JsonParser().parse(stream).asJsonObject
                return root.getAsJsonArray("issue").map { Issue(it) }
            } else {
                throw RuntimeException(method.responseBodyAsLoggedString())
            }
        } finally {
            method.releaseConnection()
        }
    }
}