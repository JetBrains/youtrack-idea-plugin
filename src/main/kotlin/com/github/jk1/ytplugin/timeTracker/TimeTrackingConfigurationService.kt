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
import org.apache.http.HttpStatus
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit


class TimeTrackingConfigurationService {

    fun getAvailableWorkItemsTypes(repo: YouTrackServer): Collection<String> {
        return TimeTrackerRestClient(repo).getAvailableWorkItemTypes().keys
    }

    // todo: make it return something meaningful
    fun addManuallyNewWorkItem(dateNotFormatted: String, selectedType: String, selectedId: String,
                               repo: YouTrackServer, comment: String, time: String): Int {

        val sdf = SimpleDateFormat("dd MMM yyyy")
        val date = sdf.parse(dateNotFormatted)
        val trackerNote = TrackerNotification()
        return try {
            TimeTrackerConnector(repo, repo.project).postWorkItemToServer(selectedId, time, selectedType,
                comment, date.time.toString())
            trackerNote.notify("Spent time was successfully added for $selectedId", NotificationType.INFORMATION)
            ComponentAware.of(repo.project).issueWorkItemsStoreComponent[repo].update(repo)
            200
        } catch (e: Exception) {
            logger.warn("Time was not posted. See IDE logs for details.")
            trackerNote.notify("Time was not posted, please check your connection", NotificationType.WARNING)
            HttpStatus.SC_BAD_REQUEST
        }
    }

    private fun configureTimerForTracking(timeTrackingDialog: SetupDialog, project: Project) {

        val timer = ComponentAware.of(project).timeTrackerComponent
        val timeToSchedule = timeTrackingDialog.scheduledTime

        val inactivityTime = TimeUnit.HOURS.toMillis(timeTrackingDialog.inactivityHours.toLong()) +
                TimeUnit.MINUTES.toMillis(timeTrackingDialog.inactivityMinutes.toLong())

        logger.debug("Manual mode is selected: ${timeTrackingDialog.manualModeCheckbox.isSelected}")
        logger.debug("Auto mode is selected: ${timeTrackingDialog.autoTrackingEnabledCheckBox.isSelected}")

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
                    (timer.isManualTrackingEnabled || timer.isAutoTrackingEnabled)) {
                notifySelectTask()
            } else {
                if (timer.isAutoTrackingEnabled) {
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