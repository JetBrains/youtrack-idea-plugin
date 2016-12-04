package com.github.jk1.ytplugin.issues.actions

import com.github.jk1.ytplugin.tasks.TaskManagerProxyComponent
import com.github.jk1.ytplugin.tasks.TaskManagerProxyComponent.Companion.CONFIGURE_SERVERS_ACTION_ID
import com.intellij.openapi.actionSystem.*
import javax.swing.JComponent


class IssueActionGroup(val parent: JComponent) : DefaultActionGroup() {

    fun add(action: IssueAction) {
        action.register(parent)
        super.add(action)
    }

    fun addConfigureTaskServerAction() {
        val action = ActionManager.getInstance().getAction(CONFIGURE_SERVERS_ACTION_ID)
        action.registerCustomShortcutSet(CustomShortcutSet.fromString("ctrl shift Q"), parent)
        super.add(action)
    }

    fun createToolbar(): JComponent = ActionManager.getInstance()
            .createActionToolbar(ActionPlaces.TOOLWINDOW_TITLE, this, false)
            .component
}