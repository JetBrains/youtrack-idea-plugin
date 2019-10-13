package com.github.jk1.ytplugin.issues.actions

import com.github.jk1.ytplugin.ui.YouTrackPluginIcons
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys.EDITOR
import com.intellij.openapi.project.DumbAware

class CreateIssueAction : AnAction(
        "Create YouTrack Issue",
        "Creates new YouTrack issue draft from a code fragment selected in the editor",
        YouTrackPluginIcons.YOUTRACK
), DumbAware {

    override fun actionPerformed(event: AnActionEvent) {
        val text = getSelectedText(event)
        if (!text.isNullOrBlank()){
            // POST /admin/users/me/drafts
            // BrowserUtil.browse("...")
        }
    }

    override fun update(event: AnActionEvent) {
        event.presentation.isVisible = !getSelectedText(event).isNullOrBlank()
    }

    private fun getSelectedText(event: AnActionEvent): String? {
        return EDITOR.getData(event.dataContext)?.caretModel?.currentCaret?.selectedText
    }
}