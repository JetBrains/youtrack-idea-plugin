package com.github.jk1.ytplugin.timeTracker.actions

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.logger
import com.github.jk1.ytplugin.timeTracker.TimeTracker
import com.github.jk1.ytplugin.timeTracker.TrackerNotification
import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.WindowManager

class SaveTrackerAction {

    fun saveTimer(project: Project, taskId: String) {

        val trackerNote = TrackerNotification()
        val timer = ComponentAware.of(project).timeTrackerComponent

        try {
            val savedTimeStorage =  ComponentAware.of(project).spentTimePerTaskStorage
            savedTimeStorage.setSavedTimeForLocalTask(taskId,
                System.currentTimeMillis() - timer.startTime - timer.pausedTime)

            val updatedTime = savedTimeStorage.getSavedTimeForLocalTask(taskId)
            if (savedTimeStorage.getSavedTimeForLocalTask(taskId) >= 60000) {
                trackerNote.notify(
                    "Tracked time for ${ComponentAware.of(project).taskManagerComponent.getActiveTask()}" +
                            " saved: ${TimeTracker.formatTimePeriodToMinutes(updatedTime)} min",
                    NotificationType.INFORMATION
                )
            }

            timer.reset()
            timer.isPaused = true

            val store: PropertiesComponent = PropertiesComponent.getInstance(project)
            store.saveFields(timer)

            val bar = WindowManager.getInstance().getStatusBar(project)
            bar?.removeWidget("Time Tracking Clock")

        } catch (e: IllegalStateException) {
            logger.warn("Time tracking exception: ${e.message}")
            logger.debug(e)
            trackerNote.notify("Could not stop time tracking: timer is not running", NotificationType.WARNING)
        }

    }
}