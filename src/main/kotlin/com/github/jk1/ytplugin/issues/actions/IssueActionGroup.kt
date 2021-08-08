package com.github.jk1.ytplugin.issues.actions

import com.github.jk1.ytplugin.commands.OpenSetupWindowAction
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.CustomShortcutSet
import com.intellij.openapi.actionSystem.DefaultActionGroup
import javax.swing.JComponent


class IssueActionGroup(private val parent: JComponent) : DefaultActionGroup() {

    override fun isDumbAware() = true

    fun add(action: IssueAction) {
        action.register(parent)
        super.add(action)
    }

    fun addConfigureTaskServerAction(repo: YouTrackServer, fromTracker: Boolean) {
        // action wrap is required to override shortcut for a global action
        val action = OpenSetupWindowAction(repo, fromTracker)
        action.registerCustomShortcutSet(CustomShortcutSet.fromString("ctrl shift Q"), parent)
        super.add(action)
    }

    fun createVerticalToolbarComponent(target: JComponent) = createToolbarComponent(target, false)

    fun createHorizontalToolbarComponent(target: JComponent) = createToolbarComponent(target, true)

    private fun createToolbarComponent(target: JComponent, horizontal: Boolean): JComponent = ActionManager.getInstance()
        .createActionToolbar(ActionPlaces.TOOLWINDOW_TITLE, this, horizontal)
        .also { it.setTargetComponent(target) }
        .component
}