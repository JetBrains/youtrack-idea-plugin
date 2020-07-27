package com.github.jk1.ytplugin.rest

import com.github.jk1.ytplugin.tasks.YouTrackServer
import org.apache.commons.httpclient.methods.GetMethod
import org.jdom.input.SAXBuilder

// todo: convert me to use json api
interface AdminRestClientBase{

    fun getVisibilityGroups(issueId: String): List<String>

    fun getAccessibleProjects(): List<String>
}