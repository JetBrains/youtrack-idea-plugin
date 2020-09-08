package com.github.jk1.ytplugin.timeTracker.actions

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.timeTracker.IconLoader
import com.github.jk1.ytplugin.whenActive
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent



class ToggleStartAndPauseTrackerAction  : AnAction(
        "Pause work timer",
        "Pause work timer",
        IconLoader.loadIcon("icons/time_tracker_pause_dark.png")){

    private var IS_START_ON = false

    fun getStartState(isStartOn: Boolean){
        IS_START_ON = isStartOn
    }

    init {
        templatePresentation.icon = when (IS_START_ON) {
            true -> AllIcons.Actions.Profile
            false -> IconLoader.loadIcon("icons/time_tracker_pause_dark.png")
        }
    }


    override fun actionPerformed(event: AnActionEvent) {
        event.whenActive {
            val project = event.project
            if (project != null) {
                val timer = ComponentAware.of(event.project!!).timeTrackerComponent
                if (!timer.isPaused && timer.isRunning && !IS_START_ON){
                    if (timer.isAutoTrackingEnable){
                        timer.startTime = timer.activityTracker?.startInactivityTime!!
                        timer.isAutoTrackingTemporaryDisabled = true
                    }
                    timer.pause()
                    event.presentation.icon = AllIcons.Actions.Profile
                    IS_START_ON = true
                    event.presentation.text = "Start work timer"
                    event.presentation.description = "Start work timer"
                } else {
                    ComponentAware.of(event.project!!).timeTrackerComponent.isAutoTrackingTemporaryDisabled = false
                    StartTrackerAction().startTracking(event.project!!)
                    event.presentation.icon = IconLoader.loadIcon("icons/time_tracker_pause_dark.png")
                    IS_START_ON = false
                    event.presentation.text = "Pause work timer"
                    event.presentation.description = "Pause work timer"
                }
            }
        }
    }
}
