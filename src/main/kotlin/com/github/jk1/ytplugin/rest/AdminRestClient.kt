package com.github.jk1.ytplugin.rest

import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.intellij.openapi.project.Project
import org.apache.commons.httpclient.methods.GetMethod
import org.jdom.input.SAXBuilder

// todo: convert me to use json api
class AdminRestClient(override val project: Project) : RestClientTrait, ResponseLoggerTrait {

    fun getVisibilityGroups(server: YouTrackServer, issueId: String): List<String> {
        val getGroupsUrl = "${server.url}/rest/issueInternal/visibilityGroups/$issueId"
        val method = GetMethod(getGroupsUrl)
        return connect(method) {
            val status = createHttpClient(server).executeMethod(method)
            val defaultGroups = listOf("All Users")
            if (status == 200) {
                val root = SAXBuilder().build(method.responseBodyAsLoggedStream())
                val groupElements = root.rootElement.children
                defaultGroups + groupElements.map {
                    it.getAttribute("name").value
                }
            } else if (status == 404) {
                // YouTrack 5.2 has no rest method to get visibility groups
                defaultGroups
            } else {
                throw RuntimeException(method.responseBodyAsLoggedString())
            }
        }
    }

    fun getAccessibleProjects(server: YouTrackServer): List<String> {
        val method = GetMethod("${server.url}/rest/admin/project")
        return connect(method) {
            val status = createHttpClient(server).executeMethod(method)
            if (status == 200) {
                val root = SAXBuilder().build(method.responseBodyAsLoggedStream())
                val projectElements = root.rootElement.children
                projectElements.map {
                    it.getAttribute("id").value
                }
            }  else {
                throw RuntimeException(method.responseBodyAsLoggedString())
            }
        }
    }
}