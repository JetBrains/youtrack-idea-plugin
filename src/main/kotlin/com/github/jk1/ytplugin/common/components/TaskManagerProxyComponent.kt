package com.github.jk1.ytplugin.common.components

import com.github.jk1.ytplugin.common.YouTrackServer
import com.intellij.concurrency.JobScheduler
import com.intellij.openapi.components.AbstractProjectComponent
import com.intellij.openapi.project.Project
import com.intellij.tasks.Task
import com.intellij.tasks.TaskManager
import com.intellij.tasks.TaskRepository
import com.intellij.tasks.impl.BaseRepository
import java.util.*
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * Provides integration with task management plugin.
 * Encapsulates plugin api details to decouple the rest of the plugin from it.
 */
class TaskManagerProxyComponent(val project: Project) : AbstractProjectComponent(project) {

    companion object {
        const val CONFIGURE_SERVERS_ACTION_ID = "tasks.configure.servers"
    }

    private var configurationHash = 0L
    private val listeners = ArrayList<() -> Unit>()
    private lateinit var timedRefreshTask: ScheduledFuture<*>

    override fun projectOpened() {
        syncTaskManagerConfig()
        timedRefreshTask = JobScheduler.getScheduler().scheduleWithFixedDelay({
            if (listeners.isNotEmpty()) {
                syncTaskManagerConfig()
            }
        }, 5, 5, TimeUnit.SECONDS)
    }

    override fun projectClosed() {
        listeners.clear()
        timedRefreshTask.cancel(false)
    }

    fun addListener(listener: () -> Unit) {
        listeners.add(listener)
    }

    fun getActiveTask(): Task {
        val task = getTaskManager().activeTask
        if (task.isIssue && task.repository?.isYouTrack() ?: false) {
            return getTaskManager().activeTask
        } else {
            throw NoActiveYouTrackTaskException()
        }
    }

    fun setActiveTask(task: Task) {
        getTaskManager().activateTask(task, true)
    }

    fun getActiveYouTrackRepository(): YouTrackServer {
        val repository = getActiveTask().repository as BaseRepository
        if (repository.isConfigured && repository.isYouTrack()) {
            return YouTrackServer(repository)
        } else {
            throw NoYouTrackRepositoryException()
        }
    }

    fun getAllConfiguredYouTrackRepositories(): List<YouTrackServer> {
        return getTaskManager().allRepositories.filter { it.isYouTrack() }.map { YouTrackServer(it as BaseRepository) }
    }

    private fun syncTaskManagerConfig() {
        synchronized(this) {
            val newHash = getTaskManager().allRepositories.fold(0L, { sum, it -> sum + it.hashCode() })
            if (configurationHash != newHash) {
                configurationHash = newHash
                listeners.forEach { it.invoke() }
            }
        }
    }

    private fun getTaskManager(): TaskManager {
        return project.getComponent(TaskManager::class.java)
                ?: throw TaskManagementDisabledException()
    }

    private fun TaskRepository.isYouTrack() = this.javaClass.name.contains("youtrack", true)
}