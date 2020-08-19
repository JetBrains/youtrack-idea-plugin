package com.github.jk1.ytplugin.issues

import com.github.jk1.ytplugin.ComponentAware
import com.intellij.concurrency.JobScheduler
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
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
        timedRefreshTask.cancel(true)
    }

    fun subscribe(listener: () -> Unit) {
        listeners.add(listener)
    }

    fun onAfterUpdate() {
        listeners.forEach { it.invoke() }
    }
}