package com.github.jk1.ytplugin.setupWindow.Actions

import com.github.jk1.ytplugin.setupWindow.SetupDialog
import com.github.jk1.ytplugin.whenActive
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import javax.swing.Icon

class OpenSetupWindowAction(): SetupAction() {

    override val text = "Open setup window"
    override val description = "Open window for configuration settings"
    override val icon: Icon = AllIcons.Debugger.Console
    override val shortcut = "control alt shift Q"

    override fun actionPerformed(event: AnActionEvent) {
        event.whenActive { project ->
            SetupDialog(project).show()
        }
    }

}