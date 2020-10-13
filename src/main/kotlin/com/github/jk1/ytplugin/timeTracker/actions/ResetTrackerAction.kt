package com.github.jk1.ytplugin.timeTracker.actions

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.ui.YouTrackPluginIcons
import com.github.jk1.ytplugin.whenActive
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent


class ResetTrackerAction : AnAction(
        "Reset work timer",
        "Reset work timer for ongoing task tracking",
        YouTrackPluginIcons.YOUTRACK_RESET_TIME_TRACKER) {

    override fun actionPerformed(event: AnActionEvent) {
        event.whenActive { project ->
            val timer = project.let { it1 -> ComponentAware.of(it1).timeTrackerComponent }
            timer.reset()
            val store: PropertiesComponent = PropertiesComponent.getInstance(project)
            store.saveFields(timer)
        }
    }

    override fun update(event: AnActionEvent) {
        val project = event.project
        if (project != null) {
            val timer = ComponentAware.of(project).timeTrackerComponent
            event.presentation.isEnabled = timer.isRunning
            event.presentation.isVisible = (timer.isManualTrackingEnable || timer.isAutoTrackingEnable)
        }
    }
}
