package com.github.jk1.ytplugin.timeTracker

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.issues.model.Issue
import com.github.jk1.ytplugin.logger
import com.github.jk1.ytplugin.rest.AdminRestClient
import com.github.jk1.ytplugin.rest.MulticatchException.Companion.multicatchException
import com.github.jk1.ytplugin.rest.TimeTrackerRestClient
import com.github.jk1.ytplugin.setup.SetupDialog
import com.github.jk1.ytplugin.tasks.NoActiveYouTrackTaskException
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.github.jk1.ytplugin.timeTracker.actions.StartTrackerAction
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.WindowManager
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.Callable
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit


class TimeTrackingConfigurator {

    fun getAvailableWorkItemsTypes(repo: YouTrackServer): Collection<String> {
        return TimeTrackerRestClient(repo).getAvailableWorkItemTypes().keys
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

    fun checkIfTrackingIsEnabledForIssue(repo: YouTrackServer, selectedIssueIndex: Int, ids: List<Issue>): Future<*> {
        return ApplicationManager.getApplication().executeOnPooledThread(
            Callable {
                try {
                    if (selectedIssueIndex != -1)
                        AdminRestClient(repo).checkIfTrackingIsEnabled(ids[selectedIssueIndex].projectName)
                    else false
                } catch (e: Exception) {
                    e.multicatchException(SocketException::class.java,
                        UnknownHostException::class.java, SocketTimeoutException::class.java) {
                        logger.warn("Exception in manual time tracker: ${e.message}")
                    }
                }
            })
    }

    private fun notifySelectTask() {
        val note = "To start using time tracking please select active task on the toolbar" +
                " or by pressing Shift + Alt + T"
        val trackerNote = TrackerNotification()
        trackerNote.notifyWithHelper(note, NotificationType.INFORMATION, OpenActiveTaskSelection())
    }

}