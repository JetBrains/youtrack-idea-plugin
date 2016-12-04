package com.github.jk1.ytplugin.issues.actions

import com.github.jk1.ytplugin.issues.model.Issue
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ide.CopyPasteManager
import org.apache.commons.lang.StringEscapeUtils
import java.awt.datatransfer.StringSelection

/**
 * Checks, if issue description contains an exception and enables analyze stack trace
 * action for that exception. Current implementation can recognize only one exception
 * per issue and ignores comments.
 *
 * todo: add configurable option to skip an unscramble dialog and analyze stacktrace right away
 */
class AnalyzeStacktraceAction(val getSelectedIssue: () -> Issue?) : IssueAction() {

    override val text = "Analyze Stacktrace"
    override val description = "Open analyze stacktrace dialog for stacktrace from issue description"
    override val icon = AllIcons.Debugger.ThreadStates.Exception!!
    override val shortcut = "control shift S"

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project
        if (project != null && project.isInitialized) {
            val issue = getSelectedIssue.invoke()
            if (issue != null && issue.hasException()) {
                openAnalyzeDialog(issue, event)
            }
        }
    }

    override fun update(event: AnActionEvent) {
        val issue = getSelectedIssue.invoke()
        event.presentation.isEnabled = issue != null && issue.hasException()
    }

    private fun openAnalyzeDialog(issue: Issue, event: AnActionEvent) {
        val clipboard = CopyPasteManager.getInstance()
        val existingContent = clipboard.contents
        // Analyze dialog uses clipboard contents, let's put a stack trace there
        clipboard.setContents(StringSelection(issue.getException()))
        ActionManager.getInstance().getAction("Unscramble").actionPerformed(event)
        if (existingContent != null) {
            // and restore everything afterwards
            clipboard.setContents(existingContent)
        }
    }

    private fun Issue.hasException() = description.contains("<pre class=\"wiki-exception\"")

    private fun Issue.getException() = StringEscapeUtils.unescapeHtml(description
            .split("</pre>")[1]
            .replace("<br/>", "\n")
            .replace("<[^>]+>".toRegex(), "")
            .replace("&hellip;", ""))
}