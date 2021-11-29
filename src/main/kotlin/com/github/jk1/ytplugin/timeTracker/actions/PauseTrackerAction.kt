package com.github.jk1.ytplugin.timeTracker.actions

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.ui.YouTrackPluginIcons
import com.github.jk1.ytplugin.whenActive
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent


class PauseTrackerAction : AnAction(
        "Pause Work Timer",
        "Pause work timer for ongoing task tracking",
        YouTrackPluginIcons.YOUTRACK_PAUSE_TIME_TRACKER) {

    override fun actionPerformed(event: AnActionEvent) {
        event.whenActive {project ->
            val timer = ComponentAware.of(project).timeTrackerComponent
            if (timer.isAutoTrackingEnabled) {
                timer.isAutoTrackingTemporaryDisabled = true
            }
            timer.pause("Work timer paused")
            val store: PropertiesComponent = PropertiesComponent.getInstance(project)
            store.saveFields(timer)
        }
    }

    override fun update(event: AnActionEvent) {
        val project = event.project
        if (project != null) {
            val timer = ComponentAware.of(event.project!!).timeTrackerComponent
            event.presentation.isVisible = (!timer.isPaused && timer.isRunning
                    && (timer.isManualTrackingEnabled || timer.isAutoTrackingEnabled)) &&
                    !timer.isAutoTrackingTemporaryDisabled
        }
    }
}