package com.github.jk1.ytplugin.scriptsDebugConfiguration.actions

import com.github.jk1.ytplugin.whenActive
import com.github.jk1.ytplugin.scriptsDebugConfiguration.ui.ConfirmScriptsLoadDialog
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class LoadScriptsAction : AnAction(
        "Load Scripts",
        "Load remote scripts to the local files",
        AllIcons.Actions.Download) {

    override fun actionPerformed(event: AnActionEvent) {
        event.whenActive { project ->
            val dialog = ConfirmScriptsLoadDialog(project)
            dialog.show()
        }
    }

}