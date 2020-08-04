package com.github.jk1.ytplugin.rest

import com.github.jk1.ytplugin.tasks.YouTrackServer
import org.apache.commons.httpclient.methods.GetMethod
import org.jdom.input.SAXBuilder


// todo: convert me to use json api
class AdminOldRestClient(override val repository: YouTrackServer) : AdminRestClientBase, RestClientTrait, ResponseLoggerTrait {

    override fun getVisibilityGroups(issueId: String): List<String> {
        val getGroupsUrl = "${repository.url}/rest/issueInternal/visibilityGroups/$issueId"
        val method = GetMethod(getGroupsUrl)
        return method.connect {
            val status = httpClient.executeMethod(method)
            val defaultGroups = listOf("All Users")
            when (status) {
                200 -> {
                    val root = SAXBuilder().build(method.responseBodyAsLoggedStream())
                    val groupElements = root.rootElement.children
                    defaultGroups + groupElements.map {
                        it.getAttribute("name").value
                    }
                }
                404 -> // YouTrack 5.2 has no rest method to get visibility groups
                    defaultGroups
                else -> throw RuntimeException(method.responseBodyAsLoggedString())
            }
        }
    }

    override fun getAccessibleProjects(): List<String> {
        val method = GetMethod("${repository.url}/rest/admin/project")
        return method.connect {
            val status = httpClient.executeMethod(method)
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