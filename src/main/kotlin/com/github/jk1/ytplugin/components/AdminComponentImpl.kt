package com.github.jk1.ytplugin.components

import com.github.jk1.ytplugin.rest.AdminRestClient
import com.intellij.openapi.components.AbstractProjectComponent
import com.intellij.openapi.project.Project

public class AdminComponentImpl(override val project : Project) :
        AbstractProjectComponent(project), AdminComponent, ComponentAware {

    val restClient = AdminRestClient(project)

    override fun getUserGroups(): List<String> {
        val login = taskManagerComponent.getYouTrackRepository().username
        return restClient.getUserGroups(login)
    }
}
