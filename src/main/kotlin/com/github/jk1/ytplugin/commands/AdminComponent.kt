package com.github.jk1.ytplugin.commands

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.rest.AdminRestClient
import com.intellij.openapi.components.AbstractProjectComponent
import com.intellij.openapi.project.Project

class AdminComponent(override val project : Project) : AbstractProjectComponent(project), ComponentAware {

    val restClient = AdminRestClient(project)

    fun getActiveTaskVisibilityGroups(): List<String> {
        val repo = taskManagerComponent.getActiveYouTrackRepository()
        val taskId = taskManagerComponent.getActiveTask().id
        return restClient.getVisibilityGroups(repo, taskId)
    }
}
