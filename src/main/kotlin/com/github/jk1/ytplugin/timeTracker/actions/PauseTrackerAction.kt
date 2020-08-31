package com.github.jk1.ytplugin.timeTracker.actions

import com.github.jk1.ytplugin.issues.actions.IssueAction
import com.github.jk1.ytplugin.rest.IssuesRestClient
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.github.jk1.ytplugin.timeTracker.TimeTracker
import com.github.jk1.ytplugin.timeTracker.TrackerNotification
import com.github.jk1.ytplugin.timeTracker.TrackerNotifier
import com.github.jk1.ytplugin.whenActive
import com.intellij.icons.AllIcons
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.tasks.TaskManager
import javax.swing.Icon
import javax.swing.ImageIcon

class PauseTrackerAction(private val timer: TimeTracker) : IssueAction() {
    override val text = "Pause work timer"
    override val description = "Pause work timer"
    override var icon: Icon = ImageIcon(this::class.java.classLoader.getResource("icons/time_tracker_pause_dark.png"))
    override val shortcut = "control shift M"


    override fun actionPerformed(event: AnActionEvent) {
            event.whenActive {
                timer.pause()
            }
        }
}
