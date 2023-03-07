package com.github.jk1.ytplugin.timeTracker.actions

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.logger
import com.github.jk1.ytplugin.rest.TimeTrackerRestClient
import com.github.jk1.ytplugin.tasks.NoYouTrackRepositoryException
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.github.jk1.ytplugin.timeTracker.TimeTrackerConnector
import com.github.jk1.ytplugin.timeTracker.TrackerNotification
import com.github.jk1.ytplugin.ui.YouTrackPluginIcons
import com.github.jk1.ytplugin.whenActive
import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.WindowManager
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit


class StopTrackerAction : AnAction(
        "Stop Work Timer",
        "Stop tracking and post spent time to the current issue",
        YouTrackPluginIcons.YOUTRACK_STOP_TIME_TRACKER) {

    override fun actionPerformed(event: AnActionEvent) {
        event.whenActive { project ->
            try {
                stopTimer(project, ComponentAware.of(project).taskManagerComponent.getActiveYouTrackRepository())
            } catch (e: NoYouTrackRepositoryException){

                val timer = ComponentAware.of(project).timeTrackerComponent
                timer.stop()

                // save time in case of exceptions
                val time = TimeUnit.MINUTES.toMillis(timer.recordedTime.toLong())
                ComponentAware.of(project).spentTimePerTaskStorage.resetSavedTimeForLocalTask(timer.issueId) // not to sum up the same item
                ComponentAware.of(project).spentTimePerTaskStorage.setSavedTimeForLocalTask(timer.issueId, time)

                timer.isAutoTrackingTemporaryDisabled = true

                logger.warn("Time could not be posted as the issue does not belong to current YouTrack instance. " +
                        "Time is saved locally: ${e.message}")
                logger.debug(e)
                val trackerNote = TrackerNotification()
                trackerNote.notify("Could not post time as the issue does not belong to the current YouTrack " +
                        "instance. Time ${timer.recordedTime} is saved locally", NotificationType.WARNING)
            }
        }
    }

    override fun update(event: AnActionEvent) {
        val project = event.project
        if (project != null) {
            val timer = ComponentAware.of(event.project!!).timeTrackerComponent
            event.presentation.isEnabled = timer.isRunning
            event.presentation.isVisible = (timer.isManualTrackingEnabled || timer.isAutoTrackingEnabled)
            if (timer.isAutoTrackingEnabled){
                event.presentation.icon = YouTrackPluginIcons.YOUTRACK_POST_FROM_TIME_TRACKER
                event.presentation.description = "Post spent time to the current issue and continue tracking"
                event.presentation.text = "Post Time to Server"
            } else {
                event.presentation.icon = YouTrackPluginIcons.YOUTRACK_STOP_TIME_TRACKER
                event.presentation.description = "Stop tracking and post spent time to the current issue"
                event.presentation.text = "Stop Work Timer"
            }
        }
    }


    fun stopTimer(project: Project, repo: YouTrackServer) {

        val trackerNote = TrackerNotification()
        val timer = ComponentAware.of(project).timeTrackerComponent

        try {
            timer.stop()

            val bar = WindowManager.getInstance().getStatusBar(project)
            bar?.removeWidget("Time Tracking Clock")

            val recordedTime = timer.recordedTime
            if (recordedTime == "0")
                trackerNote.notify("Spent time shorter than 1 minute is excluded from time tracking", NotificationType.WARNING)
            else {
                try {
                     ApplicationManager.getApplication().executeOnPooledThread (
                        Callable {
                            TimeTrackerConnector(repo, project).postWorkItemToServer(timer.issueId, recordedTime,
                                timer.type, timer.comment, (Date().time).toString(), mapOf())

                            ComponentAware.of(project).issueWorkItemsStoreComponent[repo].update(repo)
                        })
                } catch (e: Exception) {
                    logger.warn("Time tracking might not be enabled: ${e.message}")
                    logger.debug(e)

                    // save time in case of exceptions
                    val time = TimeUnit.MINUTES.toMillis(timer.recordedTime.toLong())
                    ComponentAware.of(project).spentTimePerTaskStorage.resetSavedTimeForLocalTask(timer.issueId) // not to sum up the same item
                    ComponentAware.of(project).spentTimePerTaskStorage.setSavedTimeForLocalTask(timer.issueId, time)
                }
                val store: PropertiesComponent = PropertiesComponent.getInstance(project)
                store.saveFields(timer)
            }
        } catch (e: IllegalStateException) {
            logger.warn("Time tracking exception: ${e.message}")
            logger.debug(e)
            trackerNote.notify("Could not stop time tracking: timer is not started", NotificationType.WARNING)
        }

    }
}