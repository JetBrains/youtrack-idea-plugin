package com.github.jk1.ytplugin.issues.actions

import com.github.jk1.ytplugin.issues.model.Issue
import com.github.jk1.ytplugin.whenActive
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ide.CopyPasteManager
import java.awt.datatransfer.StringSelection


class CopyIssueLinkAction(private val getSelectedIssue: () -> Issue?) : IssueAction() {

    override val text = "Copy Issue Link"
    override val description = "Copy the URL for the selected issue to the clipboard"
    override val icon = AllIcons.Actions.Copy
    override val shortcut = "control shift C"

    override fun actionPerformed(event: AnActionEvent) {
        event.whenActive {
            val issue = getSelectedIssue.invoke()
            // youtrack issues always have a url defined
            if (issue != null) {
                CopyPasteManager.getInstance().setContents(StringSelection(issue.url))
            }
        }
    }

    override fun update(event: AnActionEvent) {
        event.presentation.isEnabled = getSelectedIssue.invoke() != null
    }
}