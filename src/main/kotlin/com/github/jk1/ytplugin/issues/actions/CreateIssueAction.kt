package com.github.jk1.ytplugin.issues.actions

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.rest.IssuesRestClient
import com.github.jk1.ytplugin.ui.YouTrackPluginIcons
import com.github.jk1.ytplugin.whenActive
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys.EDITOR
import com.intellij.openapi.project.DumbAware

class CreateIssueAction : AnAction(
        "Create Issue",
        "Create a new issue draft in YouTrack from the selected content",
        YouTrackPluginIcons.YOUTRACK
), DumbAware {

    override fun actionPerformed(event: AnActionEvent) {
        val text = getSelectedText(event)
        if (!text.isNullOrBlank()){
            event.whenActive { project ->
                val repo = ComponentAware.of(project).taskManagerComponent.getAllConfiguredYouTrackRepositories().firstOrNull()
                if (repo != null) {
                    val id = IssuesRestClient(repo).createDraft(text.asCodeBlock())
                    BrowserUtil.browse("${repo.url}/newIssue?draftId=$id")
                }
            }
        }
    }

    private fun String.asCodeBlock() = "```\n$this\n```"

    override fun update(event: AnActionEvent) {
        val project = event.project ?: return
        event.presentation.isVisible = !getSelectedText(event).isNullOrBlank() &&
                ComponentAware.of(project).taskManagerComponent.getAllConfiguredYouTrackRepositories().isNotEmpty()
    }

    private fun getSelectedText(event: AnActionEvent): String? {
        return EDITOR.getData(event.dataContext)?.caretModel?.currentCaret?.selectedText
    }
}