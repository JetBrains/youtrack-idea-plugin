package com.github.jk1.ytplugin.components

import com.github.jk1.ytplugin.NoActiveYouTrackTaskFoundException
import com.github.jk1.ytplugin.TaskManagementDisabledException
import com.github.jk1.ytplugin.YouTrackRepositoryNotConfiguredException
import com.intellij.openapi.components.AbstractProjectComponent
import com.intellij.openapi.project.Project
import com.intellij.tasks.Task
import com.intellij.tasks.TaskManager
import com.intellij.tasks.TaskRepository
import com.intellij.tasks.impl.BaseRepository
import com.intellij.tasks.impl.BaseRepositoryImpl
import org.apache.commons.httpclient.HttpClient


class TaskManagerProxyComponentImpl(val project: Project) :
        AbstractProjectComponent(project), TaskManagerProxyComponent {

    override fun getActiveTask(): Task {
        val task = getTaskManager().activeTask
        if (task.isIssue && task.repository?.isYouTrack() ?: false) {
            return getTaskManager().activeTask
        } else {
            throw NoActiveYouTrackTaskFoundException()
        }
    }

    override fun getTaskManager(): TaskManager {
        return project.getComponent(TaskManager::class.java)
                ?: throw TaskManagementDisabledException()
    }

    override fun getYouTrackRepository(): BaseRepository {
        val repository = getActiveTask().repository as BaseRepository
        if (repository.isConfigured) {
            return repository
        } else {
            throw YouTrackRepositoryNotConfiguredException()
        }
    }

    override fun getRestClient(): HttpClient {
        // dirty hack to get preconfigured http client from task management plugin
        // we don't want to handle all the connection/testing/proxy stuff ourselves
        val method = BaseRepositoryImpl::class.java.getDeclaredMethod("getHttpClient")
        method.isAccessible = true
        return method.invoke(getYouTrackRepository()) as HttpClient
    }

    private fun TaskRepository.isYouTrack() = this.javaClass.name.contains("youtrack", true)
}