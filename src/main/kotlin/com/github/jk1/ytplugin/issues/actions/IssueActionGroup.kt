package com.github.jk1.ytplugin.issues.actions

import com.github.jk1.ytplugin.commands.OpenSetupWindowAction
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.intellij.openapi.actionSystem.*
import javax.swing.JComponent


class IssueActionGroup(private val parent: JComponent) : DefaultActionGroup() {

    override fun isDumbAware() = true

    fun add(action: IssueAction) {
        action.register(parent)
        super.add(action)
    }

    fun addConfigureTaskServerAction(repo: YouTrackServer) {
        // action wrap is required to override shortcut for a global action
        val action = OpenSetupWindowAction(repo)
        action.registerCustomShortcutSet(CustomShortcutSet.fromString("ctrl shift Q"), parent)
        super.add(action)
    }

    fun createVerticalToolbarComponent() = createToolbarComponent(false)

    fun createHorizontalToolbarComponent() = createToolbarComponent(true)

    private fun createToolbarComponent(horizontal: Boolean): JComponent = ActionManager.getInstance()
            .createActionToolbar(ActionPlaces.TOOLWINDOW_TITLE, this, horizontal)
            .component
}