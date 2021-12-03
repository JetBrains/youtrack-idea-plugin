package com.github.jk1.ytplugin.timeTracker.actions

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.logger
import com.github.jk1.ytplugin.tasks.NoActiveYouTrackTaskException
import com.github.jk1.ytplugin.tasks.NoYouTrackRepositoryException
import com.github.jk1.ytplugin.timeTracker.*
import com.github.jk1.ytplugin.whenActive
import com.intellij.icons.AllIcons
import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.WindowManager


class StartTrackerAction : AnAction(
    "Start Work Timer",
    "Start tracking time for the issue referenced in the active changelist",
    AllIcons.Actions.Profile
) {

    fun startAutomatedTracking(project: Project, timer: TimeTracker) {
        if (!ComponentAware.of(project).taskManagerComponent.getActiveTask().isDefault && timer.isAutoTrackingEnabled) {
            startTracking(project, timer)
        }
    }

    override fun actionPerformed(event: AnActionEvent) {
        event.whenActive { project ->
            val timer = ComponentAware.of(project).timeTrackerComponent
            try {
                ComponentAware.of(project).taskManagerComponent.getActiveYouTrackTask()
                if (!ComponentAware.of(project).taskManagerComponent.getActiveTask().isDefault) {
                    ComponentAware.of(project).timeTrackerComponent.isAutoTrackingTemporaryDisabled = false

                    if (!timer.isPaused)
                        timer.reset()
                    startTracking(project, timer)
                } else {
                    notifySelectTask()
                }
            } catch (e: NoActiveYouTrackTaskException) {
                logger.debug("Active task is not valid")
                notifySelectTask()
            }
        }
    }

    private fun notifySelectTask() {
        val note = "To start using time tracking please select active task on the toolbar" +
                " or by pressing Shift + Alt + T"
        val trackerNote = TrackerNotification()
        trackerNote.notifyWithHelper(note, NotificationType.INFORMATION, OpenActiveTaskSelection())
    }

    override fun update(event: AnActionEvent) {
        val project = event.project
        if (project != null) {
            val timer = ComponentAware.of(event.project!!).timeTrackerComponent
            event.presentation.isVisible = (timer.isPaused || !timer.isRunning || timer.isAutoTrackingTemporaryDisabled)
                    && (timer.isManualTrackingEnabled || timer.isAutoTrackingEnabled)
        }
    }


    private fun startTracking(project: Project, myTimer: TimeTracker) {
        val parentDisposable = project.getService(IssueWorkItemsStoreUpdaterService::class.java)
        if (!myTimer.isRunning) {
            myTimer.startTime = System.currentTimeMillis()
        }

        //  after manual pause only manual start is supported
        if (!myTimer.isAutoTrackingTemporaryDisabled) {
            val bar = WindowManager.getInstance().getStatusBar(project)
            if (bar?.getWidget("Time Tracking Clock") == null) {
                bar?.addWidget(TimerWidget(myTimer, parentDisposable, project), parentDisposable)
            }

            if (myTimer.isAutoTrackingEnabled) {
                myTimer.activityTracker = ActivityTracker(
                    parentDisposable = parentDisposable,
                    timer = myTimer,
                    inactivityPeriod = myTimer.inactivityPeriodInMills,
                    project = project
                )
                myTimer.activityTracker!!.startTracking()
            }

            if (!myTimer.isRunning || myTimer.isPaused) {
                try {
                    val activeTask = ComponentAware.of(project).taskManagerComponent.getActiveYouTrackTask()
                    myTimer.pausedTime = System.currentTimeMillis() - myTimer.startTime - myTimer.timeInMills
                    myTimer.issueId = activeTask.id
                    myTimer.issueIdReadable = activeTask.id
                    myTimer.start(activeTask.id)
                } catch (e: NoActiveYouTrackTaskException) {
                    val trackerNote = TrackerNotification()
                    trackerNote.notify("Could not record time: not a valid YouTrack issue", NotificationType.WARNING)
                    trackerNote.notifyWithHelper(
                        "To use time tracking please select valid active task on the toolbar" +
                                " or by pressing Shift + Alt + T",
                        NotificationType.INFORMATION,
                        OpenActiveTaskSelection()
                    )
                } catch (e: NoYouTrackRepositoryException) {
                    val trackerNote = TrackerNotification()
                    trackerNote.notify(
                        "Unable to start automatic time tracking, please select" +
                                " valid active task first", NotificationType.WARNING
                    )
                }
            } else {
                val trackerNote = TrackerNotification()
                trackerNote.notify(
                    "Work timer is running for ${myTimer.issueIdReadable} ",
                    NotificationType.INFORMATION
                )
            }
            myTimer.isAutoTrackingTemporaryDisabled = false
        }
        val store: PropertiesComponent = PropertiesComponent.getInstance(project)
        store.saveFields(myTimer)
    }

}