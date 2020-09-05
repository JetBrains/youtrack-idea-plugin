package com.github.jk1.ytplugin.timeTracker.actions

import com.github.jk1.ytplugin.issues.actions.IssueAction
import com.github.jk1.ytplugin.timeTracker.TimeTracker
import com.github.jk1.ytplugin.timeTracker.TrackerNotification
import com.github.jk1.ytplugin.whenActive
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import javax.swing.Icon
import javax.swing.ImageIcon

class ResetTrackerAction(timer: TimeTracker) : IssueAction() {
    override val text = "Reset work timer"
    override val description = "Reset work timer"
    override var icon: Icon = ImageIcon(this::class.java.classLoader.getResource("icons/time_tracker_reset_dark.png"))
    override val shortcut = "control shift N"
    private val myTimer = timer

    override fun actionPerformed(event: AnActionEvent) {
        event.whenActive {
            if (myTimer.isRunning){
                myTimer.isRunning = false
                myTimer.isPaused = false
                myTimer.recordedTime = "0"
                myTimer.timeInMills = 0
                myTimer.startTime = System.currentTimeMillis()
                val trackerNote = TrackerNotification()
                trackerNote.notify("Work timer reset", NotificationType.INFORMATION)
            } else {
                val trackerNote = TrackerNotification()
                trackerNote.notify("Could not reset - timer is not started", NotificationType.ERROR)
            }
        }
    }
}
