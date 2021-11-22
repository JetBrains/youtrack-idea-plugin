package com.github.jk1.ytplugin.timeTracker.actions

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.logger
import com.github.jk1.ytplugin.rest.TimeTrackerRestClient
import com.github.jk1.ytplugin.tasks.NoYouTrackRepositoryException
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.github.jk1.ytplugin.timeTracker.TrackerNotification
import com.github.jk1.ytplugin.ui.YouTrackPluginIcons
import com.github.jk1.ytplugin.whenActive
import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.WindowManager
import java.util.*


class StopTrackerAction : AnAction(
        "Stop Work Timer",
        "Stop tracking and post spent time to the current issue",
        YouTrackPluginIcons.YOUTRACK_STOP_TIME_TRACKER) {

    override fun actionPerformed(event: AnActionEvent) {
        event.whenActive { project ->
            stopTimer(project)
        }
    }

    override fun update(event: AnActionEvent) {
        val project = event.project
        if (project != null) {
            val timer = ComponentAware.of(event.project!!).timeTrackerComponent
            event.presentation.isEnabled = timer.isRunning
            event.presentation.isVisible = (timer.isManualTrackingEnable || timer.isAutoTrackingEnable)
            if (timer.isAutoTrackingEnable){
                event.presentation.icon = YouTrackPluginIcons.YOUTRACK_POST_FROM_TIME_TRACKER
                event.presentation.description = "Post spent time to the current issue and continue tracking"
                event.presentation.text = "Post Time To Server"
            } else {
                event.presentation.icon = YouTrackPluginIcons.YOUTRACK_STOP_TIME_TRACKER
                event.presentation.description = "Stop tracking and post spent time to the current issue"
                event.presentation.text = "Stop Work Timer"
            }
        }
    }


    fun stopTimer(project: Project) {

        val trackerNote = TrackerNotification()
        val timer = ComponentAware.of(project).timeTrackerComponent

        val repo: YouTrackServer?
        try {
            repo = ComponentAware.of(project).taskManagerComponent.getActiveYouTrackRepository()
            logger.debug("YouTrack server integration is configured")
        } catch (e: NoYouTrackRepositoryException){
            logger.debug("YouTrack server integration is not configured yet")
            return
        }

        try {
            timer.stop()
            val bar = WindowManager.getInstance().getStatusBar(project)
            bar?.removeWidget("Time Tracking Clock")

            val recordedTime = timer.recordedTime
            if (recordedTime == "0")
                trackerNote.notify("Spent time shorter than 1 minute is excluded from time tracking", NotificationType.WARNING)
            else {
                try {
                    TimeTrackerRestClient(repo).postNewWorkItem(timer.issueId, recordedTime, timer.type, timer.comment,
                        (Date().time).toString())

                    trackerNote.notify("Work timer stopped, spent time added to" +
                            " ${timer.issueIdReadable}", NotificationType.INFORMATION)
                    ComponentAware.of(project).issueWorkItemsStoreComponent[repo].update(repo)
                    val store: PropertiesComponent = PropertiesComponent.getInstance(project)
                    store.saveFields(timer)
                } catch (e: Exception) {
                    logger.warn("Time tracking might not be enabled: ${e.message}")
                    logger.debug(e)
                    trackerNote.notify("Could not send time to YouTrack, please check you connection and " +
                            "the validity of active task. Recorded time: ${timer.recordedTime} min", NotificationType.WARNING)
                }
            }
        } catch (e: IllegalStateException) {
            logger.warn("Time tracking exception: ${e.message}")
            logger.debug(e)
            trackerNote.notify("Could not stop time tracking: timer is not started", NotificationType.WARNING)
        }

    }
}