package com.github.jk1.ytplugin.issues.actions

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware

/**
 * Starts async issue store update from a remote server
 */
class RefreshIssuesAction(val repo: YouTrackServer) : AnAction("Refresh issues",
        "Update issue list from YouTrack server",
        AllIcons.Actions.Refresh), DumbAware {

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project
        if (project != null && project.isInitialized) {
            ComponentAware.of(project).issueStoreComponent[repo].update()
        }
    }

    override fun update(event: AnActionEvent) {
        val project = event.project
        event.presentation.isEnabled = project != null &&
                project.isInitialized &&
                !ComponentAware.of(project).issueStoreComponent[repo].isUpdating()
    }
}