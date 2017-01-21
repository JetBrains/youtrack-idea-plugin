package com.github.jk1.ytplugin.issues

import com.github.jk1.ytplugin.ComponentAware
import com.intellij.concurrency.JobScheduler
import com.intellij.openapi.components.AbstractProjectComponent
import com.intellij.openapi.project.Project
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import javax.swing.SwingUtilities

/**
 * Project-scoped issue store maintenance component.
 *  - Manages timed issue store updates for active projects
 *  - Removes stale and obsolete data upon task management configuration changes
 */
class IssueStoreUpdaterComponent(override val project: Project) : AbstractProjectComponent(project), ComponentAware {

    private lateinit var timedRefreshTask: ScheduledFuture<*>

    override fun initComponent() {
        //  todo: customizable update interval
        timedRefreshTask = JobScheduler.getScheduler().scheduleWithFixedDelay({
            SwingUtilities.invokeLater {
                taskManagerComponent.getAllConfiguredYouTrackRepositories().forEach {
                    issueStoreComponent[it].update()
                }
            }
        }, 5, 5, TimeUnit.MINUTES)
    }

    override fun disposeComponent() {
        timedRefreshTask.cancel(false)
    }
}