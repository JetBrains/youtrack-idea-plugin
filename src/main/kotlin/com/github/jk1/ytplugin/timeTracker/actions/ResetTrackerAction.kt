package com.github.jk1.ytplugin.timeTracker.actions

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.timeTracker.IconLoader
import com.github.jk1.ytplugin.timeTracker.TrackerNotification
import com.github.jk1.ytplugin.whenActive
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project


class ResetTrackerAction : AnAction(
        "Reset work timer",
        "Reset work timer",
        IconLoader.loadIcon("icons/time_tracker_reset_dark.png")) {

    override fun actionPerformed(event: AnActionEvent) {
        event.whenActive {
            val project = event.project
            if (project != null) {
                val timer = ComponentAware.of(project).timeTrackerComponent
                timer.reset()
            }
        }
    }
}
