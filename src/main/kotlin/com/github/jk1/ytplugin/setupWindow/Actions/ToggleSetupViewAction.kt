package com.github.jk1.ytplugin.setupWindow.Actions

import com.github.jk1.ytplugin.ui.SetupList
import com.intellij.icons.AllIcons
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import javax.swing.Icon


class ToggleSetupViewAction(val project: Project, private val issueList: SetupList) : SetupAction() {

    override val text = "Toggle Presentation"
    override val description = "Toggle issue list presentation detail level"
    override val icon: Icon = AllIcons.Actions.Collapseall
    override val shortcut = "control shift T"

    private val DATA_KEY = "ytplugin.issueListCompactView"
    private val store: PropertiesComponent = PropertiesComponent.getInstance(project)

    init {
        val compactView = store.getBoolean(DATA_KEY)
        issueList.renderer.compactView = compactView
        templatePresentation.icon = when (compactView) {
            true -> AllIcons.Actions.Expandall
            false -> AllIcons.Actions.Collapseall
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        if (issueList.renderer.compactView) {
            store.setValue(DATA_KEY, false)
            issueList.renderer.compactView = false
            e.presentation.icon = AllIcons.Actions.Collapseall
        } else {
            store.setValue(DATA_KEY, true)
            issueList.renderer.compactView = true
            e.presentation.icon = AllIcons.Actions.Expandall
        }
        issueList.update()
    }
}