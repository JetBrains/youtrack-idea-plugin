package com.github.jk1.ytplugin.timeTracker.actions

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.timeTracker.AllSavedTimerItemsDialog
import com.github.jk1.ytplugin.ui.YouTrackPluginIcons
import com.github.jk1.ytplugin.whenActive
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.DialogWrapper.DialogWrapperAction
import com.intellij.util.net.HttpConfigurable
import java.awt.event.ActionEvent


class ShowAllSavedTimeTrackingItems : AnAction(
    "Show Saved Time Tracking Items",
    "Show time tracking items that have not been posted to YouTrack yet",
    YouTrackPluginIcons.YOUTRACK_RESET_TIME_TRACKER // TODO custom icon
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


