package com.github.jk1.ytplugin.rest

import com.intellij.openapi.project.Project
import org.apache.commons.httpclient.methods.GetMethod
import org.jdom.input.SAXBuilder


class AdminRestClient(override val project: Project) : AbstractRestClient(project) {

    fun getUserGroups(issueId: String): List<String> {
        val baseUrl = taskManagerComponent.getYouTrackRepository().url
        val getUsersUrl = "$baseUrl/rest/issueInternal/visibilityGroups/$issueId"
        val method = GetMethod(getUsersUrl)

        try {
            val status = createHttpClient().executeMethod(method)
            if (status == 200) {
                val root = SAXBuilder().build(method.responseBodyAsStream)
                val groupElements=root.rootElement.children
                return groupElements.map {
                   it.getAttribute("name").value
                }
            } else {
                throw RuntimeException(method.responseBodyAsString)
            }
        } finally {
            method.releaseConnection()
        }
    }
}