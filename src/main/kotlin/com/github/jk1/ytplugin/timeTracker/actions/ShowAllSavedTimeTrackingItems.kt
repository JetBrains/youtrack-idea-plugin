package com.github.jk1.ytplugin.timeTracker.actions

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.timeTracker.AllSavedTimerItemsDialog
import com.github.jk1.ytplugin.whenActive
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent


class ShowAllSavedTimeTrackingItems : AnAction(
    "Show Saved Time Tracking Items",
    "Show time tracking items that have not been posted to YouTrack yet",
     AllIcons.Vcs.History // TODO custom icon
) {

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project
        event.whenActive {
            val repo = ComponentAware.of(project!!).taskManagerComponent.getActiveYouTrackRepository()
            AllSavedTimerItemsDialog(project, repo).show()
        }
    }

    override fun update(event: AnActionEvent) {
        val project = event.project
        if (project != null) {
            val timer = ComponentAware.of(project).timeTrackerComponent
            event.presentation.isEnabled = timer.isManualTrackingEnable || timer.isAutoTrackingEnable
            event.presentation.isVisible = timer.isManualTrackingEnable || timer.isAutoTrackingEnable
        }
    }
}


