package com.github.jk1.ytplugin.issues.actions

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.rest.IssuesRestClient
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
            val project = event.project
            if (project?.isDisposed == false) {
                val repo = ComponentAware.of(project).taskManagerComponent.getAllConfiguredYouTrackRepositories().firstOrNull()
                if (repo != null) {
                    // todo: fenced code block
                    val id = IssuesRestClient(repo).createDraft(text)
                    BrowserUtil.browse("${repo.url}/newIssue?draftId=$id")
                } else {
                    // todo: notification
                }
            }
        }
    }

    override fun update(event: AnActionEvent) {
        event.presentation.isVisible = !getSelectedText(event).isNullOrBlank()
    }

    private fun getSelectedText(event: AnActionEvent): String? {
        return EDITOR.getData(event.dataContext)?.caretModel?.currentCaret?.selectedText
    }
}