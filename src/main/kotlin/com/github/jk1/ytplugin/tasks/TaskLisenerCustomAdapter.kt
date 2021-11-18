package com.github.jk1.ytplugin.tasks

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.timeTracker.OnTaskSwitchingTimerDialog
import com.github.jk1.ytplugin.timeTracker.OpenActiveTaskSelection
import com.github.jk1.ytplugin.timeTracker.TrackerNotification
import com.github.jk1.ytplugin.timeTracker.actions.SaveTrackerAction
import com.github.jk1.ytplugin.timeTracker.actions.StartTrackerAction
import com.github.jk1.ytplugin.timeTracker.actions.StopTrackerAction
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.tasks.LocalTask
import com.intellij.tasks.TaskListener
import com.github.jk1.ytplugin.logger

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

            logger.debug("Switch from: ${spentTimePerTaskStorage.getSavedTimeForLocalTask(timeTrackerComponent.issueId)}")
            if (spentTimePerTaskStorage.getSavedTimeForLocalTask(timeTrackerComponent.issueId) > 0){
                OnTaskSwitchingTimerDialog(project, taskManagerComponent.getActiveYouTrackRepository()).show()
            }
        }
    }


    override fun taskAdded(task: LocalTask) {
        // second condition is required for the post on vcs commit functionality - it is disabled in manual tracking
        // mode, but taskAdded triggers on git action 9with the same task) and thus timer is stopped even in manual mode.
        // However, we still want to post time on task switching in manual mode so (isAutoTrackingEnabled == true)
        // is not an option here
        if (timeTrackerComponent.isRunning && combineTaskIdAndSummary(taskManagerComponent.getActiveTask()) != task.summary) {
            SaveTrackerAction().saveTimer(project, taskManagerComponent.getActiveTask())
            StopTrackerAction().stopTimer(project)
        }
    }

    private fun combineTaskIdAndSummary(task: LocalTask) = "${task.id} ${task.summary}"

    override fun taskRemoved(task: LocalTask) {
    }
}