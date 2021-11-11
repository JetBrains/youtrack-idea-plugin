package com.github.jk1.ytplugin.tasks

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.timeTracker.OpenActiveTaskSelection
import com.github.jk1.ytplugin.timeTracker.TrackerNotification
import com.github.jk1.ytplugin.timeTracker.actions.StartTrackerAction
import com.github.jk1.ytplugin.timeTracker.actions.StopTrackerAction
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.tasks.LocalTask
import com.intellij.tasks.TaskListener

class TaskListenerCustomAdapter(override val project: Project) : TaskListener, ComponentAware {


    override fun taskDeactivated(task: LocalTask) {
        try {
            taskManagerComponent.getActiveYouTrackTask()
        } catch (e: NoActiveYouTrackTaskException) {
            val note = "To start using time tracking please select active task on the toolbar" +
                    " or by pressing Shift + Alt + T"
            val trackerNote = TrackerNotification()
            trackerNote.notifyWithHelper(note, NotificationType.INFORMATION, OpenActiveTaskSelection())
        }

    }

    override fun taskActivated(task: LocalTask) {
        if (timeTrackerComponent.isAutoTrackingTemporaryDisabled) {
            timeTrackerComponent.isAutoTrackingTemporaryDisabled = false
            StartTrackerAction().startAutomatedTracking(project, timeTrackerComponent)
        }
    }


    override fun taskAdded(task: LocalTask) {
        if (timeTrackerComponent.isRunning) {
            StopTrackerAction().stopTimer(project)
        }
    }

    override fun taskRemoved(task: LocalTask) {
    }
}