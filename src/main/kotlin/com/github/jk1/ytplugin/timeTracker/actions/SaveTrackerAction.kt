package com.github.jk1.ytplugin.timeTracker.actions

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.logger
import com.github.jk1.ytplugin.timeTracker.TimeTracker
import com.github.jk1.ytplugin.timeTracker.TrackerNotification
import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.WindowManager
import com.intellij.tasks.LocalTask

class SaveTrackerAction {

    fun saveTimer(project: Project, task: LocalTask) {

        val trackerNote = TrackerNotification()
        val timer = ComponentAware.of(project).timeTrackerComponent

        try {
            val savedTimeStorage =  ComponentAware.of(project).spentTimePerTaskStorage
            savedTimeStorage.setSavedTimeForLocalTask(task, timer.timeInMills)

            trackerNote.notify("Switched from issue ${ComponentAware.of(project).taskManagerComponent.getActiveTask()}, " +
                    "time ${TimeTracker.formatTimePeriod(savedTimeStorage.getSavedTimeForLocalTask(task))} min is saved.",
                NotificationType.INFORMATION)

            timer.reset()
            timer.isPaused = true

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