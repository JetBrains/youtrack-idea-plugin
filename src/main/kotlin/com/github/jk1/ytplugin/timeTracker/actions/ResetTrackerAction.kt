package com.github.jk1.ytplugin.timeTracker.actions

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.ui.YouTrackPluginIcons
import com.github.jk1.ytplugin.whenActive
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent


class ResetTrackerAction : AnAction(
        "Reset work timer",
        "Reset work timer",
        YouTrackPluginIcons.YOUTRACK_RESET_TIME_TRACKER) {

    override fun actionPerformed(event: AnActionEvent) {
        event.whenActive {
            val project = event.project
            if (project != null) {
                val timer = ComponentAware.of(project).timeTrackerComponent
                timer.reset()
            }
        }
    }


    override fun update(event: AnActionEvent) {
        val project = event.project
        if (project != null) {
            val timer = ComponentAware.of(event.project!!).timeTrackerComponent
            event.presentation.isEnabled = timer.isRunning
            event.presentation.isVisible = (timer.isManualTrackingEnable || timer.isAutoTrackingEnable)
        }
    }

}
