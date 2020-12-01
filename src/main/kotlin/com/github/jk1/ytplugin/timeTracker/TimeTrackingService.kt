package com.github.jk1.ytplugin.timeTracker

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.logger
import com.github.jk1.ytplugin.rest.TimeTrackerRestClient
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.github.jk1.ytplugin.timeTracker.actions.StartTrackerAction
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.WindowManager
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit
import kotlin.time.hours
import kotlin.time.minutes
import kotlin.time.seconds


class TimeTrackingService {

    fun getAvailableWorkItemsTypes(repo: YouTrackServer): MutableList<String> {
        val types = mutableListOf<String>()
        val typesReceived = TimeTrackerRestClient(repo).getAvailableWorkItemTypes()
        if (typesReceived.isNotEmpty()) {
            typesReceived.forEach { types.add(it.name) }
        }
        return types
    }

    fun postNewWorkItem(dateNotFormatted: String, selectedType: String, selectedId: String,
                        repo: YouTrackServer, comment: String, time: String) : Int {

        val sdf = SimpleDateFormat("dd MMM yyyy")
        val date = sdf.parse(dateNotFormatted)
        val status = TimeTrackerRestClient(repo).postNewWorkItem(selectedId, time, selectedType,
                comment, date.time.toString())

        val trackerNote = TrackerNotification()
        if (status == 200) {
            val postedTIme = (date.time / 3600000).toString() + ":" + ((date.time - date.time / 3600000) /60000).toString()
            trackerNote.notify("Time $postedTIme was successfully posted on server for issue $selectedId",
                    NotificationType.INFORMATION)
            ComponentAware.of(repo.project).issueWorkItemsStoreComponent[repo].update(repo)
        } else {
            logger.warn("Time was not posted: code $status")
            trackerNote.notify("Time was not posted, please check your connection", NotificationType.WARNING)
        }

        return status
    }

    fun configureTimerForTracking(timeTrackingTab: TimeTrackerSettingsTab, repo: YouTrackServer, project: Project) {
        val timer = ComponentAware.of(repo.project).timeTrackerComponent

        val timeToSchedule = timeTrackingTab.getScheduledTime()

        val inactivityTime = TimeUnit.HOURS.toMillis(timeTrackingTab.getInactivityHours().toLong()) +
                TimeUnit.MINUTES.toMillis(timeTrackingTab.getInactivityMinutes().toLong())

        timer.setupTimer(timeTrackingTab.getComment(), timeTrackingTab.getPostWhenCommitCheckbox().isSelected,
                timeTrackingTab.getAutoTrackingEnabledCheckBox().isSelected,
                timeTrackingTab.getType().toString(), timeTrackingTab.getManualModeCheckbox().isSelected,
                timeTrackingTab.getScheduledCheckbox().isSelected, timeToSchedule,
                inactivityTime, timeTrackingTab.getPostOnClose().isSelected)
        timer.timeInMills = 0
        timer.pausedTime = 0
        timer.isAutoTrackingTemporaryDisabled = false
        if (timer.isAutoTrackingEnable) {
            StartTrackerAction().startAutomatedTracking(project, timer)
        } else {
            val bar = project.let { it1 -> WindowManager.getInstance().getStatusBar(it1) }
            bar?.removeWidget("Time Tracking Clock")
            timer.activityTracker?.dispose()
        }
    }

}