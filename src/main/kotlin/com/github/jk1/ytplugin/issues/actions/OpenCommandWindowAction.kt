package com.github.jk1.ytplugin.issues.actions

import com.github.jk1.ytplugin.issues.model.Issue
import com.github.jk1.ytplugin.ui.CommandDialog
import com.github.jk1.ytplugin.whenActive
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import javax.swing.Icon

class OpenCommandWindowAction(private val getSelectedIssue: () -> Issue?): IssueAction() {

    override val text = "Open Command Dialog"
    override val description = "Update the selected issue by applying a command"
    override val icon: Icon = AllIcons.Debugger.Console
    override val shortcut = "control shift Y"

    override fun actionPerformed(event: AnActionEvent) {
        event.whenActive { project ->
            val issue = getSelectedIssue.invoke()
            if (issue != null) {
                CommandDialog(project, issue).show()
            }
        }
    }

    override fun update(event: AnActionEvent) {
        event.presentation.isEnabled = getSelectedIssue.invoke() != null
    }
}