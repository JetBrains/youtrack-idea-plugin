package com.github.jk1.ytplugin.commands.components

import com.github.jk1.ytplugin.commands.rest.AdminRestClient
import com.github.jk1.ytplugin.common.components.ComponentAware
import com.intellij.openapi.components.AbstractProjectComponent
import com.intellij.openapi.project.Project

class AdminComponent(override val project : Project) : AbstractProjectComponent(project), ComponentAware {

    val restClient = AdminRestClient(project)

    fun getUserGroups(): List<String> {
        val taskId = taskManagerComponent.getActiveTask().id
        return restClient.getUserGroups(taskId)
    }
}
