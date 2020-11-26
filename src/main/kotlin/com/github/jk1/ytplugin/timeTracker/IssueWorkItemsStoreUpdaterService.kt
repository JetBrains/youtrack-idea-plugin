package com.github.jk1.ytplugin.timeTracker

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.logger
import com.github.jk1.ytplugin.rest.TimeTrackerRestClient
import com.github.jk1.ytplugin.tasks.NoYouTrackRepositoryException
import com.intellij.concurrency.JobScheduler
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
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
        val store: PropertiesComponent = PropertiesComponent.getInstance(project)
        try {
            val repo = ComponentAware.of(project).taskManagerComponent.getActiveYouTrackRepository()
            val timer = ComponentAware.of(repo.project).timeTrackerComponent

            if (timer.isWhenProjectClosedEnabled) {
                logger.debug("state PROJECT_CLOSE with posting enabled")
                try {
                    timer.stop()
                    repo.let { it1 ->
                        TimeTrackerRestClient(it1).postNewWorkItem(timer.issueId,
                                timer.recordedTime, timer.type, timer.comment, (Date().time).toString())
                    }
                    ComponentAware.of(project).issueWorkItemsStoreComponent[repo].update(repo)
                    store.saveFields(timer)
                } catch (e: IllegalStateException) {
                    logger.debug("Could not stop time tracking: timer is not started: ${e.message}")
                }
            } else {
                try {
                    timer.pause()
                    store.saveFields(timer)
                } catch (e: IllegalStateException) {
                    logger.debug("Failed on time tracker fields storage: ${e.message}")
                }
                logger.debug("state PROJECT_CLOSE with posting disabled")
            }
            logger.debug("time tracker stopped on PROJECT_CLOSE with time ${timer.timeInMills}")

        } catch (e: NoYouTrackRepositoryException) {
            logger.warn("NoYouTrackRepository:  ${e.message}")
        }
        timedRefreshTask.cancel(true)
    }

    private fun saveState(myStore: PropertiesComponent, timer: TimeTracker) {
        if (!timer.isPaused && timer.isRunning) {
            timer.timeInMills = System.currentTimeMillis() - timer.startTime - timer.pausedTime
            timer.recordedTime = timer.formatTimePeriod(timer.timeInMills)
            timer.isPaused = true
        }
        myStore.saveFields(timer)
    }

    fun subscribe(listener: () -> Unit) {
        listeners.add(listener)
    }

    fun onAfterUpdate() {
        listeners.forEach { it.invoke() }
    }
}