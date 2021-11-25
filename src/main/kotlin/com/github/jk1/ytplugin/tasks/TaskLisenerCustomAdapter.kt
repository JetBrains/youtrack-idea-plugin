package com.github.jk1.ytplugin.tasks

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.timeTracker.OnTaskSwitchingTimerDialog
import com.github.jk1.ytplugin.timeTracker.OpenActiveTaskSelection
import com.github.jk1.ytplugin.timeTracker.TrackerNotification
import com.github.jk1.ytplugin.timeTracker.actions.SaveTrackerAction
import com.github.jk1.ytplugin.timeTracker.actions.StartTrackerAction
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.tasks.LocalTask
import com.intellij.tasks.TaskListener
import com.github.jk1.ytplugin.logger
import com.github.jk1.ytplugin.timeTracker.TimeTracker
import com.github.jk1.ytplugin.timeTracker.actions.StopTrackerAction

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

        // split cases of vcs commit and branch switching in manual/none mode
        if (!timeTrackerComponent.isAutoTrackingEnable) {
            timeTrackerComponent.pause("Work timer paused")

            val savedTimeStorage = ComponentAware.of(project).spentTimePerTaskStorage
            savedTimeStorage.setSavedTimeForLocalTask(timeTrackerComponent.issueId, timeTrackerComponent.timeInMills)

            val trackerNote = TrackerNotification()
            trackerNote.notify("Total time ${TimeTracker.formatTimePeriod(
                savedTimeStorage.getSavedTimeForLocalTask(timeTrackerComponent.issueId))} " +
                    "min for issue ${timeTrackerComponent.issueId} is saved locally", NotificationType.INFORMATION)

            timeTrackerComponent.resetTimeOnly()
            timeTrackerComponent.updateIdOnTaskSwitching()
        }

        if (timeTrackerComponent.postOnIssueSwitching){
            StopTrackerAction().stopTimer(project)
        }

        timeTrackerComponent.isAutoTrackingTemporaryDisabled = false
        StartTrackerAction().startAutomatedTracking(project, timeTrackerComponent)

        logger.debug("Switch from: ${spentTimePerTaskStorage.getSavedTimeForLocalTask(timeTrackerComponent.issueId)}")
        if (spentTimePerTaskStorage.getSavedTimeForLocalTask(task.id) > 0){
            OnTaskSwitchingTimerDialog(project, taskManagerComponent.getActiveYouTrackRepository()).show()
        }
    }


    override fun taskAdded(task: LocalTask) {
        // the second and third conditions are required for the post on vcs commit functionality - it is disabled in manual tracking
        // mode, but taskAdded triggers on git action 9with the same task) and thus timer is stopped even in manual mode.
        // However, we still want to post/save time on task switching in manual mode so saveTime call is also triggered there
        if (timeTrackerComponent.isRunning && timeTrackerComponent.isAutoTrackingEnable &&
            combineTaskIdAndSummary(taskManagerComponent.getActiveTask()) != task.summary) {
            SaveTrackerAction().saveTimer(project, taskManagerComponent.getActiveTask().id)
        }
    }

    private fun combineTaskIdAndSummary(task: LocalTask) = "${task.id} ${task.summary}"

    override fun taskRemoved(task: LocalTask) {
    }
}