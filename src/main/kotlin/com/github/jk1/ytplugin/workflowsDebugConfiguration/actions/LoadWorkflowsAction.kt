package com.github.jk1.ytplugin.workflowsDebugConfiguration.actions

import com.github.jk1.ytplugin.whenActive
import com.github.jk1.ytplugin.workflowsDebugConfiguration.ui.WorkflowNameEntryDialog
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class LoadWorkflowsAction() : AnAction(
        "Load Workflow",
        "Load remote workflow scripts to the local files",
        AllIcons.Actions.Download) {

    override fun actionPerformed(event: AnActionEvent) {
        event.whenActive { project ->
            val dialog = WorkflowNameEntryDialog(project)
            dialog.show()
        }
    }

}