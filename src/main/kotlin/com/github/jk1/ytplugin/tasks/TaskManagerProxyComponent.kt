package com.github.jk1.ytplugin.tasks

import com.github.jk1.ytplugin.issues.model.Issue
import com.intellij.concurrency.JobScheduler
import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.project.Project
import com.intellij.tasks.Task
import com.intellij.tasks.TaskManager
import com.intellij.tasks.TaskRepository
import com.intellij.tasks.actions.OpenTaskDialog
import com.intellij.tasks.impl.BaseRepository
import com.intellij.tasks.youtrack.YouTrackRepository
import java.util.*
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * Provides integration with task management plugin.
 * Encapsulates task management api details to decouple the rest of our plugin from them.
 */
class TaskManagerProxyComponent(val project: Project) : ProjectComponent {

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
        }, 3, 3, TimeUnit.SECONDS)
    }

    override fun projectClosed() {
        listeners.clear()
        timedRefreshTask.cancel(false)
    }

    fun addConfigurationChangeListener(listener: () -> Unit) {
        listeners.add(listener)
    }

    fun getActiveYouTrackTask(): Task {
        val task = getTaskManager().activeTask
        if (task.isIssue && task.repository?.isYouTrack() == true) {
            return getTaskManager().activeTask
        } else {
            throw NoActiveYouTrackTaskException()
        }
    }

    fun getActiveTask() = getTaskManager().activeTask

    fun setActiveTask(task: Task) {
        OpenTaskDialog(project, task).show()
        // todo: configurable action with command patterns
        // getTaskManager().activateTask(task, true)
    }

    fun getActiveYouTrackRepository(): YouTrackServer {
        val repository = getActiveYouTrackTask().repository as BaseRepository
        if (repository.isConfigured && repository.isYouTrack()) {
            return YouTrackServer(repository as YouTrackRepository, project)
        } else {
            throw NoYouTrackRepositoryException()
        }
    }

    fun getAllConfiguredYouTrackRepositories() = getTaskManager()
            .allRepositories
            .filter { it.isYouTrack() }
            .map { YouTrackServer(it as YouTrackRepository, project) }

    fun getYouTrackRepository(issue: Issue) =
            getAllConfiguredYouTrackRepositories()
                    .first { repo -> repo.url == issue.repoUrl }

    private fun syncTaskManagerConfig() {
        synchronized(this) {
            val newHash = getTaskManager().allRepositories
                    .filter { it.isYouTrack() }
                    .fold(0L, { sum, it -> sum + it.hashCode() })
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