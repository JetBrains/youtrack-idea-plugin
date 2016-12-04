package com.github.jk1.ytplugin.issues.actions

import com.github.jk1.ytplugin.ui.IssueListCellRenderer
import com.github.jk1.ytplugin.ui.IssueListToolWindowContent
import com.intellij.icons.AllIcons
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project


class ToggleIssueViewAction(val project: Project, val renderer: IssueListCellRenderer,
                            val listModel: IssueListToolWindowContent.IssueListModel) : IssueAction() {

    override val text = "Toggle issue list presentation"
    override val description = "Toggle issue list presentation detail level"
    override val icon = AllIcons.Actions.Collapseall!!
    override val shortcut = "control shift T"

    val DATA_KEY = "ytplugin.issueListCompactView"
    val store: PropertiesComponent = PropertiesComponent.getInstance(project)

    init {
        val compactView = store.getBoolean(DATA_KEY)
        renderer.compactView = compactView
        templatePresentation.icon = when (compactView) {
            true -> AllIcons.Actions.Expandall
            false -> AllIcons.Actions.Collapseall
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        if (renderer.compactView) {
            store.setValue(DATA_KEY, false)
            renderer.compactView = false
            e.presentation.icon = AllIcons.Actions.Collapseall
        } else {
            store.setValue(DATA_KEY, true)
            renderer.compactView = true
            e.presentation.icon = AllIcons.Actions.Expandall
        }
        listModel.update()
    }
}