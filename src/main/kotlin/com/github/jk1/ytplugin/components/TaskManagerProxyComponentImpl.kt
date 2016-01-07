package com.github.jk1.ytplugin.components

import com.intellij.openapi.components.AbstractProjectComponent
import com.intellij.openapi.project.Project
import com.intellij.tasks.Task
import com.intellij.tasks.TaskManager
import com.intellij.tasks.impl.BaseRepository
import com.intellij.tasks.impl.BaseRepositoryImpl
import com.intellij.tasks.youtrack.YouTrackRepository
import org.apache.commons.httpclient.HttpClient


class TaskManagerProxyComponentImpl(val project: Project) :
        AbstractProjectComponent(project), TaskManagerProxyComponent {

    override fun getActiveTask(): Task {
        return getTaskManager().activeTask
    }

    override fun getTaskManager(): TaskManager {
        return project.getComponent(TaskManager::class.java)
                ?: throw TaskManagementDisabledException("Task Management plugin is disabled")
    }

    override  fun getYouTrackRepository(): BaseRepository {
        val repository = getTaskManager().allRepositories
                ?.filter { it is YouTrackRepository }
                ?.firstOrNull() as BaseRepository?
                ?: throw TaskManagementDisabledException("No YouTrack server found")
        if (repository.isConfigured){
            return repository
        } else {
            throw YouTrackRepositoryNotConfiguredException("YouTrack server integration is not configured yet")
        }
    }

    override fun getRestClient(): HttpClient {
        val method = BaseRepositoryImpl::class.java.getDeclaredMethod("getHttpClient")
        //method.isAccessible = true
        return method.invoke(getYouTrackRepository()) as HttpClient
    }
}