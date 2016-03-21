package com.github.jk1.ytplugin.commands.components

import com.github.jk1.ytplugin.commands.rest.AdminRestClient
import com.intellij.openapi.components.AbstractProjectComponent
import com.intellij.openapi.project.Project

class AdminComponentImpl(override val project : Project) : AbstractProjectComponent(project), AdminComponent {

    val restClient = AdminRestClient(project)

    override fun getUserGroups(): List<String> {
        val taskId = taskManagerComponent.getActiveTask().id
        return restClient.getUserGroups(taskId)
    }
}
