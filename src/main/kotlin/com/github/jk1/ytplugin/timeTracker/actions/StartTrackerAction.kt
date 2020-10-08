package com.github.jk1.ytplugin.timeTracker.actions

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.rest.IssuesRestClient
import com.github.jk1.ytplugin.tasks.NoYouTrackRepositoryException
import com.github.jk1.ytplugin.timeTracker.ActivityTracker
import com.github.jk1.ytplugin.timeTracker.TimerWidget
import com.github.jk1.ytplugin.timeTracker.TimeTracker
import com.github.jk1.ytplugin.timeTracker.TrackerNotification
import com.github.jk1.ytplugin.whenActive
import com.intellij.icons.AllIcons
import com.intellij.internal.performance.currentLatencyRecordKey
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.WindowManager
import com.intellij.openapi.wm.impl.status.widget.StatusBarWidgetsManager
import com.intellij.tasks.TaskManager


class StartTrackerAction : AnAction(
        "Start work timer",
        "Start work timer for selected active task",
        AllIcons.Actions.Profile) {

    fun startAutomatedTracking(project: Project, timer: TimeTracker) {
        if (timer.isAutoTrackingEnable) {
            startTracking(project, timer)
        }
    }

    override fun actionPerformed(event: AnActionEvent) {
        event.whenActive { project ->
            val timer = ComponentAware.of(project).timeTrackerComponent
            ComponentAware.of(project).timeTrackerComponent.isAutoTrackingTemporaryDisabled = false
            startTracking(project, timer)
        }
    }

    override fun update(event: AnActionEvent) {
        val project = event.project
        if (project != null) {
            val timer = ComponentAware.of(event.project!!).timeTrackerComponent
            event.presentation.isVisible = (timer.isPaused || !timer.isRunning || timer.isAutoTrackingTemporaryDisabled)
                    && (timer.isManualTrackingEnable || timer.isAutoTrackingEnable)
        }
    }


    private fun startTracking(project: Project, myTimer: TimeTracker) {
        val taskManager = project.let { it1 -> TaskManager.getManager(it1) }
        val activeTask = taskManager.activeTask
        val parentDisposable = project.getService(StatusBarWidgetsManager::class.java)

        if (!myTimer.isAutoTrackingTemporaryDisabled) {
            val bar = WindowManager.getInstance().getStatusBar(project)
            if (bar?.getWidget("Time Tracking Clock") == null) {
                bar?.addWidget(TimerWidget(myTimer, parentDisposable), parentDisposable)
            }

            if (!myTimer.isRunning || myTimer.isPaused) {
                try {
                    myTimer.pausedTime = System.currentTimeMillis() - myTimer.startTime - myTimer.timeInMills
                    myTimer.issueId = IssuesRestClient.getEntityIdByIssueId(activeTask.id, project)
                    myTimer.issueIdReadable = activeTask.id
                    myTimer.startTime = System.currentTimeMillis()
//                    myTimer.startTime = System.currentTimeMillis()
                    if (myTimer.issueId == "0") {
                        val trackerNote = TrackerNotification()
                        trackerNote.notify("Could not post time: not a YouTrack issue", NotificationType.WARNING)
                    } else {
                        myTimer.start(activeTask.id)
                        // case for activity tracker enabled
                        if (myTimer.isAutoTrackingEnable) {
                            myTimer.activityTracker = ActivityTracker(
                                    parentDisposable = parentDisposable,
                                    timer = myTimer,
                                    inactivityPeriod = myTimer.inactivityPeriodInMills,
                                    project = project
                            )
                            myTimer.activityTracker!!.startTracking()
                            myTimer.isAutoTrackingEnable = true
                        }
                    }
                } catch (e: NoYouTrackRepositoryException) {
                    val trackerNote = TrackerNotification()
                    trackerNote.notify("Unable to start automatic time tracking, please select" +
                            " valid active task first", NotificationType.WARNING)
                }
            } else {
                val trackerNote = TrackerNotification()
                trackerNote.notify("Work timer is already running for issue ${myTimer.issueIdReadable} ", NotificationType.INFORMATION)
            }
        }
    }

}