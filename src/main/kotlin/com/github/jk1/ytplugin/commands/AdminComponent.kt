package com.github.jk1.ytplugin.commands

import com.github.jk1.ytplugin.rest.AdminRestClient
import com.github.jk1.ytplugin.ComponentAware
import com.intellij.openapi.components.AbstractProjectComponent
import com.intellij.openapi.project.Project

class AdminComponent(override val project : Project) : AbstractProjectComponent(project), ComponentAware {

    val restClient = AdminRestClient(project)

    fun getUserGroups(): List<String> {
        val taskId = taskManagerComponent.getActiveTask().id
        return restClient.getUserGroups(taskId)
    }
}
