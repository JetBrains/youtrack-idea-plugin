package com.github.jk1.ytplugin.issues.actions

import com.github.jk1.ytplugin.commands.CommandSession
import com.github.jk1.ytplugin.issues.model.Issue
import com.github.jk1.ytplugin.ui.CommentDialog
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import javax.swing.Icon

class AddCommentAction(private val getSelectedIssue: () -> Issue?): IssueAction() {

    override val text = "Add Comment"
    override val description = "Adds a comment to the selected issue"
    override val icon: Icon = AllIcons.Ide.Notification.NoEvents
    override val shortcut = "control shift C"

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project
        if (project != null) {
            val issue = getSelectedIssue.invoke()
            if (issue != null) {
                CommentDialog(project, CommandSession(issue)).show()
            }
        }
    }
}