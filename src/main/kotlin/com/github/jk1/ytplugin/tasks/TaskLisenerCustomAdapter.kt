package com.github.jk1.ytplugin.tasks

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.timeTracker.TrackerNotification
import com.github.jk1.ytplugin.timeTracker.actions.StopTrackerAction
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.tasks.LocalTask
import com.intellij.tasks.TaskListener

class TaskListenerCustomAdapter(val project: Project) : TaskListener {
    override fun taskDeactivated(task: LocalTask) {
        val timer = ComponentAware.of(project).timeTrackerComponent
        if (timer.isRunning && timer.isAutoTrackingEnable) {
            StopTrackerAction().stopTimer(project)
        }
    }

    override fun taskActivated(task: LocalTask) {

    }

    override fun taskAdded(task: LocalTask) {
    }

    override fun taskRemoved(task: LocalTask) {
    }
}