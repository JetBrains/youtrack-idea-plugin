package com.github.jk1.ytplugin.issues.actions

import com.github.jk1.ytplugin.timeTracker.TimeTracker
import com.github.jk1.ytplugin.whenActive
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import javax.swing.Icon


class StopTrackerAction(timer: TimeTracker) : IssueAction() {
    override val text = "Stop time tracking"
    override val description = "Stop time tracking"

//    override var icon: Icon = ImageIcon("time_tracker_stop.png");
    override var icon: Icon = AllIcons.Actions.Suspend

    override val shortcut = "control shift L"
    private val myTimer = timer

    override fun actionPerformed(event: AnActionEvent) {
        event.whenActive {
            val time = myTimer.stop()
        }
    }
}