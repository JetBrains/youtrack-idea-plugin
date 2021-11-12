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
import com.intellij.tasks.LocalTask
import java.util.*


class SaveTrackerAction {

    fun saveTimer(project: Project, task: LocalTask) {


        val trackerNote = TrackerNotification()
        val timer = ComponentAware.of(project).timeTrackerComponent

        try {
            ComponentAware.of(project).spentTimePerTaskStorage.setSavedTimeForLocalTask(task, timer.timeInMills)
            trackerNote.notify("Switched from issue ${timer.issueId}, time ${timer.recordedTime} min is recorded",
                NotificationType.INFORMATION)

            timer.stop()

            val store: PropertiesComponent = PropertiesComponent.getInstance(project)
            store.saveFields(timer)

            val bar = WindowManager.getInstance().getStatusBar(project)
            bar?.removeWidget("Time Tracking Clock")

        } catch (e: IllegalStateException) {
            logger.warn("Time tracking exception: ${e.message}")
            logger.debug(e)
            trackerNote.notify("Could not stop time tracking: timer is not started", NotificationType.WARNING)
        }

    }
}