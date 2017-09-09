package com.github.jk1.ytplugin.issues.actions

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.issues.model.Issue
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent

class PinIssueAction(private val getSelectedIssue: () -> Issue?): IssueAction() {

    override val text: String = "Pin issue"
    override val description = "Open issue in a separate pinned tab"
    override val icon = AllIcons.General.Pin_tab
    override val shortcut = "control shift P"

    override fun actionPerformed(event: AnActionEvent) {
        val issue = getSelectedIssue.invoke()
        val project = event.project
        if (issue != null && project != null) {
           ComponentAware.of(project).pluginApiComponent.openIssueInToolWidow(issue)
        }
    }

    override fun update(event: AnActionEvent) {
        event.presentation.isEnabled = getSelectedIssue.invoke() != null
    }
}