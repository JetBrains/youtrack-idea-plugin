package com.github.jk1.ytplugin

import com.github.jk1.ytplugin.common.YouTrackServer
import com.intellij.openapi.project.Project
import com.intellij.tasks.TaskManager
import com.intellij.tasks.impl.TaskManagerImpl
import com.intellij.tasks.youtrack.YouTrackRepository
import com.intellij.tasks.youtrack.YouTrackRepositoryType


interface TaskManagerTrait: IdeaProjectTrait, YouTrackConnectionTrait {

    val project: Project

    fun getTaskManagerComponent() = project.getComponent(TaskManager::class.java)!! as TaskManagerImpl

    fun createYouTrackRepository(): YouTrackServer {
        val repository = YouTrackRepository(YouTrackRepositoryType())
        repository.url = serverUrl
        repository.username = username
        repository.password = password
        repository.defaultSearch = ""
        getTaskManagerComponent().setRepositories(listOf(repository))
        //todo: mock YouTrack here server to break dependency from task-core
        return YouTrackServer(repository, project)
    }

    fun cleanUpTaskManager(){
        val taskManager = getTaskManagerComponent()
        readAction {
            taskManager.localTasks.forEach { taskManager.removeTask(it) }
        }
        taskManager.setRepositories(listOf())
    }
}