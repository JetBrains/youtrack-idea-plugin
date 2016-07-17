package com.github.jk1.ytplugin.search.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent


class SetAsActiveTaskAction : AnAction("Set as active task",
        "Create task manager task from a selected issue and switch to it",
        AllIcons.Graph.Export) {

    override fun actionPerformed(e: AnActionEvent) {
        throw UnsupportedOperationException("not implemented")
    }
}