package com.github.jk1.ytplugin.issues.actions

import com.github.jk1.ytplugin.tasks.TaskManagerProxyComponent.Companion.CONFIGURE_SERVERS_ACTION_ID
import com.github.jk1.ytplugin.toolWindow.Actions.SetupAction
import com.intellij.openapi.actionSystem.*
import javax.swing.JComponent


class IssueActionGroup(private val parent: JComponent) : DefaultActionGroup() {

    override fun isDumbAware() = true

    fun add(action: IssueAction) {
        action.register(parent)
        super.add(action)
    }

    fun addConfigureTaskServerAction() {
        // action wrap is required to override shortcut for a global action
        val action = EmptyAction.wrap(ActionManager.getInstance().getAction(CONFIGURE_SERVERS_ACTION_ID))
        action.registerCustomShortcutSet(CustomShortcutSet.fromString("ctrl shift Q"), parent)
        super.add(action)
    }

    fun createVerticalToolbarComponent() = createToolbarComponent(false)

    fun createHorizontalToolbarComponent() = createToolbarComponent(true)

    private fun createToolbarComponent(horizontal: Boolean): JComponent = ActionManager.getInstance()
            .createActionToolbar(ActionPlaces.TOOLWINDOW_TITLE, this, horizontal)
            .component
}