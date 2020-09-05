package com.github.jk1.ytplugin.timeTracker.actions

import com.github.jk1.ytplugin.issues.actions.IssueAction
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.github.jk1.ytplugin.whenActive
import com.intellij.openapi.actionSystem.AnActionEvent
import javax.swing.Icon
import javax.swing.ImageIcon

class PauseTrackerAction (val repo: YouTrackServer) : IssueAction() {
    override val text = "Pause work timer"
    override val description = "Pause work timer"
    override var icon: Icon = ImageIcon(this::class.java.classLoader.getResource("icons/time_tracker_pause_dark.png"))
    override val shortcut = "control shift M"


    override fun actionPerformed(event: AnActionEvent) {
            event.whenActive {
//                val project = event.project
//                val repo = project?.let { it1 -> ComponentAware.of(it1).taskManagerComponent.getActiveYouTrackRepository() }
                val timer = repo.timeTracker
                timer.pause()
            }
        }
}
