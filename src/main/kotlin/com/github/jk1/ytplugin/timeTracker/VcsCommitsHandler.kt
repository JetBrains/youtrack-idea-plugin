package com.github.jk1.ytplugin.timeTracker

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.timeTracker.actions.StopTrackerAction
import com.intellij.notification.NotificationType
import com.intellij.openapi.vcs.CheckinProjectPanel
import com.intellij.openapi.vcs.changes.CommitContext
import com.intellij.openapi.vcs.checkin.CheckinHandler
import com.intellij.openapi.vcs.checkin.CheckinHandlerFactory


class VcsCommitsHandler : CheckinHandlerFactory() {

    override fun createHandler(panel: CheckinProjectPanel, commitContext: CommitContext): CheckinHandler {
        return object : CheckinHandler() {
            override fun checkinSuccessful() {
                val message = panel.commitMessage
                val project = panel.project
                val timer = ComponentAware.of(project).timeTrackerComponent
                if (timer.isPostAfterCommitEnabled && timer.isAutoTrackingEnabled){
                    val trackerNote = TrackerNotification()
                    trackerNote.notify("Stop timer on commit \"${message}\"", NotificationType.INFORMATION)
                    StopTrackerAction().stopTimer(project, ComponentAware.of(project).taskManagerComponent.getActiveYouTrackRepository())
                }
            }
        }
    }
}