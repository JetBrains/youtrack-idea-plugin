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
import java.lang.IllegalStateException
import java.util.*


class StopTrackerAction : AnAction(
        "Post Time",
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
            event.presentation.text = if (timer.isAutoTrackingEnable) "Post current tracking progress" else "Post Time"
        }
    }

    fun stopTimer(project: Project) {
        val trackerNote = TrackerNotification()
        val repo: YouTrackServer?
        try {
             repo = ComponentAware.of(project).taskManagerComponent.getActiveYouTrackRepository()
             logger.debug("YouTrack server integration is configured")
        } catch (e: NoYouTrackRepositoryException){
            logger.debug("YouTrack server integration is not configured yet")
            return
        }
        val timer = ComponentAware.of(project).timeTrackerComponent

        try {
            timer.stop()
            val bar = project.let { it1 -> WindowManager.getInstance().getStatusBar(it1) }
            bar?.removeWidget("Time Tracking Clock")

            if (timer.recordedTime == "0")
                trackerNote.notify("Spent time shorter than 1 minute is excluded from time tracking", NotificationType.WARNING)
            else {
                val status = repo.let { it1 ->
                    TimeTrackerRestClient(it1).postNewWorkItem(timer.issueId,
                            timer.recordedTime, timer.type, timer.comment, (Date().time).toString())
                }
                if (status != 200){

                    logger.warn("Time tracking might not be enabled: $status")
                    trackerNote.notify("Could not record time: time tracking is disabled (status $status)", NotificationType.WARNING)
                }
                else {
                    trackerNote.notify("Work timer stopped, spent time added to" +
                            " ${timer.issueIdReadable}", NotificationType.INFORMATION)
                    ComponentAware.of(project).issueWorkItemsStoreComponent[repo].update(repo)
                    val store: PropertiesComponent = PropertiesComponent.getInstance(project)
                    store.saveFields(timer)
                }
            }
        } catch (e: IllegalStateException) {
            logger.warn("Time tracking exception: ${e.message}")
            trackerNote.notify("Could not stop time tracking: timer is not started", NotificationType.WARNING)
        }

    }
}