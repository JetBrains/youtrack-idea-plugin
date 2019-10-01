package com.github.jk1.ytplugin.issues.actions

import com.github.jk1.ytplugin.commands.CommandSession
import com.github.jk1.ytplugin.issues.model.Issue
import com.github.jk1.ytplugin.ui.CommandDialog
import com.github.jk1.ytplugin.whenActive
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import javax.swing.Icon

class OpenCommandWindowAction(private val getSelectedIssue: () -> Issue?): IssueAction() {

    override val text = "Open Command Window"
    override val description = "Apply command to a currently selected issue in a tool window"
    override val icon: Icon = AllIcons.Debugger.Console
    override val shortcut = "control shift Y"

    override fun actionPerformed(event: AnActionEvent) {
        event.whenActive { project ->
            val issue = getSelectedIssue.invoke()
            if (issue != null) {
                CommandDialog(project, CommandSession(issue)).show()
            }
        }
    }

    override fun update(event: AnActionEvent) {
        event.presentation.isEnabled = getSelectedIssue.invoke() != null
    }
}