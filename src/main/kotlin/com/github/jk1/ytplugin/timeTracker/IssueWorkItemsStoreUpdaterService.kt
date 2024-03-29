package com.github.jk1.ytplugin.timeTracker

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.logger
import com.github.jk1.ytplugin.rest.TimeTrackerRestClient
import com.github.jk1.ytplugin.tasks.NoYouTrackRepositoryException
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.intellij.concurrency.JobScheduler
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.serviceContainer.AlreadyDisposedException
import java.util.*
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import javax.swing.SwingUtilities

/**
 * Manages timed issueWorkItems store updates for active projects
 * todo: the service is initialized lazily, so it's only behave as expected because of a #subscribe call
 * todo: convert into a post startup activity
 */
@Service
class IssueWorkItemsStoreUpdaterService(override val project: Project) : Disposable, ComponentAware {

    //  todo: customizable update interval
    private val timedRefreshTask: ScheduledFuture<*> =
            JobScheduler.getScheduler().scheduleWithFixedDelay({
                SwingUtilities.invokeLater {
                    if (!project.isDisposed) {
                        taskManagerComponent.getAllConfiguredYouTrackRepositories().forEach {
                            issueWorkItemsStoreComponent[it].update(it)
                        }
                    }
                }
            }, 5, 5, TimeUnit.MINUTES)

    private val listeners: MutableSet<() -> Unit> = mutableSetOf()

    override fun dispose() {
        try {
            val repo = taskManagerComponent.getActiveYouTrackRepository()
            val timer = timeTrackerComponent

            if (timer.isWhenProjectClosedEnabled) {
                logger.debug("state PROJECT_CLOSE with posting enabled")
                postOnProjectClose(timer, repo)
            }

        } catch (e: NoYouTrackRepositoryException) {
            logger.warn("NoYouTrackRepository:  ${e.message}")
        } catch (e: AlreadyDisposedException) {
            logger.debug("Container is already disposed")
            logger.debug(e)
        }
        timedRefreshTask.cancel(true)
    }

    private fun postOnProjectClose(timer: TimeTracker, repo: YouTrackServer) {
        try {
            timer.stop()
            repo.let { it1 ->
                val connector = TimeTrackerConnector(it1, project)
                connector.postWorkItemToServer(timer.issueId, timer.recordedTime, timer.type,
                    timer.comment, (Date().time).toString(), mapOf())
                // post not only last recorded item, but all saved items as well
                connector.postSavedWorkItemsToServer(spentTimePerTaskStorage.getAllStoredItems())
            }
            val propertiesStore: PropertiesComponent = PropertiesComponent.getInstance(project)
            propertiesStore.setValue("spentTimePerTaskStorage.store", timer.spentTimePerTaskStorage.getAllStoredItems().toString())

            logger.debug("time tracker stopped on PROJECT_CLOSE with time ${timer.recordedTime}")
        } catch (e: IllegalStateException) {
            logger.debug("Could not stop time tracking: timer is not started: ${e.message}")
        }
    }

    fun subscribe(listener: () -> Unit) {
        listeners.add(listener)
    }

    fun onAfterUpdate() {
        listeners.forEach { it.invoke() }
    }
}