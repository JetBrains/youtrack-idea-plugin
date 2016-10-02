package com.github.jk1.ytplugin.rest

import com.intellij.openapi.project.Project
import org.apache.commons.httpclient.methods.GetMethod
import org.jdom.input.SAXBuilder


class AdminRestClient(override val project: Project) : RestClientTrait, ResponseLoggerTrait {

    fun getUserGroups(issueId: String): List<String> {
        val baseUrl = taskManagerComponent.getActiveYouTrackRepository().url
        val getUsersUrl = "$baseUrl/rest/issueInternal/visibilityGroups/$issueId"
        val method = GetMethod(getUsersUrl)

        try {
            val status = createHttpClient().executeMethod(method)
            if (status == 200) {
                val root = SAXBuilder().build(method.responseBodyAsLoggedStream())
                val groupElements=root.rootElement.children
                return groupElements.map {
                   it.getAttribute("name").value
                }
            } else if (status == 404) {
                // YouTrack 5.2 has no rest method to get visibility groups
                return listOf("All Users")
            } else {
                throw RuntimeException(method.responseBodyAsLoggedString())
            }
        } finally {
            method.releaseConnection()
        }
    }
}