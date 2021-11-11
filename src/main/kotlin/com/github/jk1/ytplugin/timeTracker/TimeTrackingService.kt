package com.github.jk1.ytplugin.timeTracker

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.logger
import com.github.jk1.ytplugin.rest.TimeTrackerRestClient
import com.github.jk1.ytplugin.setup.SetupDialog
import com.github.jk1.ytplugin.tasks.NoActiveYouTrackTaskException
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.github.jk1.ytplugin.timeTracker.actions.StartTrackerAction
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.WindowManager
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit


class TimeTrackingService {

    fun getAvailableWorkItemsTypes(repo: YouTrackServer): Collection<String> {
        return TimeTrackerRestClient(repo).getAvailableWorkItemTypes().keys
    }

    // todo: make it return something meaningful
    fun postNewWorkItem(dateNotFormatted: String, selectedType: String, selectedId: String,
                        repo: YouTrackServer, comment: String, time: String): Int {

        val sdf = SimpleDateFormat("dd MMM yyyy")
        val date = sdf.parse(dateNotFormatted)
        val trackerNote = TrackerNotification()
        return try {
            TimeTrackerRestClient(repo).postNewWorkItem(selectedId, time, selectedType, comment, date.time.toString())
            trackerNote.notify("Spent time was successfully added for $selectedId", NotificationType.INFORMATION)
            ComponentAware.of(repo.project).issueWorkItemsStoreComponent[repo].update(repo)
            200
        } catch (e: Exception) {
            logger.warn("Time was not posted. See IDE logs for details.")
            trackerNote.notify("Time was not posted, please check your connection", NotificationType.WARNING)
            -1
        }
    }

    private fun configureTimerForTracking(timeTrackingDialog: SetupDialog, project: Project) {

        val timer = ComponentAware.of(project).timeTrackerComponent
        val timeToSchedule = timeTrackingDialog.scheduledTime

        val inactivityTime = TimeUnit.HOURS.toMillis(timeTrackingDialog.inactivityHours.toLong()) +
                TimeUnit.MINUTES.toMillis(timeTrackingDialog.inactivityMinutes.toLong())

        timer.setupTimerProperties(timeTrackingDialog.autoTrackingEnabledCheckBox.isSelected,
                timeTrackingDialog.manualModeCheckbox.isSelected, timeTrackingDialog.scheduledCheckbox.isSelected,
                timeToSchedule, inactivityTime)

        timer.timeInMills = 0
        timer.pausedTime = 0
        timer.isAutoTrackingTemporaryDisabled = false

    }

    fun setupTimeTracking(timeTrackingDialog: SetupDialog, project: Project) {

        val timer = ComponentAware.of(project).timeTrackerComponent

        configureTimerForTracking(timeTrackingDialog, project)

        try {
            if (ComponentAware.of(project).taskManagerComponent.getActiveTask().isDefault &&
                    (timer.isManualTrackingEnable || timer.isAutoTrackingEnable)) {
                notifySelectTask()
            } else {
                if (timer.isAutoTrackingEnable) {
                    try {
                        ComponentAware.of(project).taskManagerComponent.getActiveYouTrackTask()
                        StartTrackerAction().startAutomatedTracking(project, timer)
                    } catch (e: NoActiveYouTrackTaskException) {
                        notifySelectTask()
                    }
                } else {
                    val bar = project.let { it1 -> WindowManager.getInstance().getStatusBar(it1) }
                    bar?.removeWidget("Time Tracking Clock")
                    timer.activityTracker?.dispose()
                }
            }

        } catch (e: NoActiveYouTrackTaskException) {
            notifySelectTask()
        }
    }

    private fun notifySelectTask() {
        val note = "To start using time tracking please select active task on the toolbar" +
                " or by pressing Shift + Alt + T"
        val trackerNote = TrackerNotification()
        trackerNote.notifyWithHelper(note, NotificationType.INFORMATION, OpenActiveTaskSelection())
    }

}