package com.github.jk1.ytplugin.rest

import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.intellij.openapi.project.Project
import org.apache.commons.httpclient.methods.GetMethod
import org.jdom.input.SAXBuilder


class AdminRestClient(override val project: Project) : RestClientTrait, ResponseLoggerTrait {

    fun getVisibilityGroups(server: YouTrackServer, issueId: String): List<String> {
        val getUsersUrl = "${server.url}/rest/issueInternal/visibilityGroups/$issueId"
        val method = GetMethod(getUsersUrl)
        return connect(method) {
            val status = createHttpClient().executeMethod(method)
            if (status == 200) {
                val root = SAXBuilder().build(method.responseBodyAsLoggedStream())
                val groupElements=root.rootElement.children
                groupElements.map {
                    it.getAttribute("name").value
                }
            } else if (status == 404) {
                // YouTrack 5.2 has no rest method to get visibility groups
                listOf("All Users")
            } else {
                throw RuntimeException(method.responseBodyAsLoggedString())
            }
        }
    }
}