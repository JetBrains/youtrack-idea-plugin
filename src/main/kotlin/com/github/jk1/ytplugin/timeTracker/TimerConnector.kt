package com.github.jk1.ytplugin.timeTracker

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.rest.TimeTrackerRestClient
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.github.jk1.ytplugin.timeTracker.actions.StartTrackerAction
import com.intellij.openapi.project.Project
import java.util.concurrent.TimeUnit


class TimerConnector {

    fun getAvailableWorkItemsTypes(repo: YouTrackServer) : MutableList<String> {
        val types = mutableListOf<String>()
        val typesReceived =  TimeTrackerRestClient(repo).getAvailableWorkItemTypes()
        if (typesReceived.isNotEmpty()) {
            typesReceived.map { types.add(it.name) }
        }
        return types
    }

    fun prepareConnectedTab(timeTrackingTab: TimeTrackerSettingsTab, repo: YouTrackServer, project: Project) {
        val timer = ComponentAware.of(repo.project).timeTrackerComponent

        val timeToSchedule = timeTrackingTab.getScheduledHours() + ":" +
                timeTrackingTab.getScheduledMinutes() + ":00"

        val inactivityTime = TimeUnit.HOURS.toMillis(timeTrackingTab.getInactivityHours().toLong()) +
                TimeUnit.MINUTES.toMillis(timeTrackingTab.getInactivityMinutes().toLong())

        timer.setupTimer(timeTrackingTab.getComment(), timeTrackingTab.getPostWhenCommitCheckbox().isSelected,
                timeTrackingTab.getAutoTrackingEnabledCheckBox().isSelected,
                timeTrackingTab.getType().toString(), timeTrackingTab.getManualModeCheckbox().isSelected,
                timeTrackingTab.getScheduledCheckbox().isSelected, timeToSchedule,
                inactivityTime, timeTrackingTab.getPostOnClose().isSelected, repo)

        if (timer.isAutoTrackingEnable) {
            StartTrackerAction().startAutomatedTracking(project, timer)
        } else {
            timer.activityTracker?.dispose()
        }
    }

}