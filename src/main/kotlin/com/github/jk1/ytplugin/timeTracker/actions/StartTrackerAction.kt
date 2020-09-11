package com.github.jk1.ytplugin.timeTracker.actions

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.rest.IssuesRestClient
import com.github.jk1.ytplugin.timeTracker.*
import com.github.jk1.ytplugin.whenActive
import com.intellij.icons.AllIcons
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.WindowManager
import com.intellij.tasks.TaskManager

class StartTrackerAction : AnAction(
        "Start work timer",
        "Start work timer",
        AllIcons.Actions.Profile) {

    fun startAutomatedTracking(project: Project, timer: TimeTracker) {
        if (timer.isAutoTrackingEnable){
            startTracking(project, timer)
        }
    }

    override fun actionPerformed(event: AnActionEvent) {
        event.whenActive {
            if (event.project != null) {
                val timer = ComponentAware.of(event.project!!).timeTrackerComponent
                ComponentAware.of(event.project!!).timeTrackerComponent.isAutoTrackingTemporaryDisabled = false
                startTracking(event.project!!, timer)
            }
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

        if (!myTimer.isAutoTrackingTemporaryDisabled) {
            if (!myTimer.isRunning || myTimer.isPaused) {
                myTimer.issueId = IssuesRestClient.getEntityIdByIssueId(activeTask.id, project)
                myTimer.issueIdReadable = activeTask.id
                if (myTimer.issueId == "0") {
                    val trackerNote = TrackerNotification()
                    trackerNote.notify("Could not post time: not a YouTrack issue", NotificationType.ERROR)
                } else {
                    val bar = WindowManager.getInstance().getStatusBar(project)
                    if (bar?.getWidget("Time Tracking Clock") == null) {
                        bar?.addWidget(ClockWidget(myTimer))
                    }
                    myTimer.start(activeTask.id)
                    // case for ctivity tracker enabled
                    if (myTimer.isAutoTrackingEnable) {
                        val application = ApplicationManager.getApplication()
                        myTimer.activityTracker = ActivityTracker(
                                parentDisposable = application,
                                timer = myTimer,
                                inactivityPeriod = myTimer.inactivityPeriodInMills,
                                project = project
                        )
                        myTimer.activityTracker!!.startTracking()
                        myTimer.isAutoTrackingEnable = true
                    }
                }
            } else {
                val trackerNote = TrackerNotification()
                trackerNote.notify("Work timer is already running for issue ${myTimer.issueIdReadable} ", NotificationType.ERROR)
            }
        }
    }
}