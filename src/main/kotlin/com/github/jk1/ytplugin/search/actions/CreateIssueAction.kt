package com.github.jk1.ytplugin.search.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent


class CreateIssueAction: AnAction("Create issue",
        "Report a new issue to YouTrack",
        AllIcons.ToolbarDecorator.Add) {

    override fun actionPerformed(e: AnActionEvent?) {
        throw UnsupportedOperationException("not implemented")
    }
}