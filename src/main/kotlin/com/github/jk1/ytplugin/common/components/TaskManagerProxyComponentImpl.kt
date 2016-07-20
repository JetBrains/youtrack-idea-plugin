package com.github.jk1.ytplugin.common.components

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
            throw NoActiveYouTrackTaskException()
        }
    }

    override fun setActiveTask(task: Task) {
        getTaskManager().activateTask(task, true)
    }

    override fun getActiveYouTrackRepository(): BaseRepository {
        val repository = getActiveTask().repository as BaseRepository
        if (repository.isConfigured && repository.isYouTrack()) {
            return repository
        } else {
            throw NoYouTrackRepositoryException()
        }
    }

    override fun getAllConfiguredYouTrackRepositories(): List<BaseRepository> {
        val youTracks = getTaskManager().allRepositories.filter { it.isYouTrack() }.map { it as BaseRepository }
        if (youTracks.isEmpty()) {
            throw NoYouTrackRepositoryException()
        } else {
            return youTracks
        }
    }

    override fun getRestClient(): HttpClient {
        return getRestClient(getActiveYouTrackRepository())
    }

    override fun getRestClient(repository: BaseRepository): HttpClient {
        // dirty hack to get preconfigured http client from task management plugin
        // we don't want to handle all the connection/testing/proxy stuff ourselves
        val method = BaseRepositoryImpl::class.java.getDeclaredMethod("getHttpClient")
        method.isAccessible = true
        return method.invoke(repository) as HttpClient
    }

    private fun getTaskManager(): TaskManager {
        return project.getComponent(TaskManager::class.java)
                ?: throw TaskManagementDisabledException()
    }

    private fun TaskRepository.isYouTrack() = this.javaClass.name.contains("youtrack", true)
}