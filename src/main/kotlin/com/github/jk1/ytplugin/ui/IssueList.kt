package com.github.jk1.ytplugin.ui

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.issues.model.Issue
import com.github.jk1.ytplugin.runAction
import com.github.jk1.ytplugin.tasks.TaskManagerProxyComponent
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.intellij.ui.ListSpeedSearch
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBLoadingPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBScrollPane.HORIZONTAL_SCROLLBAR_NEVER
import com.intellij.ui.components.JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
import java.awt.BorderLayout
import java.awt.event.ActionListener
import javax.swing.AbstractListModel
import javax.swing.KeyStroke
import javax.swing.SwingUtilities

class IssueList(val repo: YouTrackServer) : JBLoadingPanel(BorderLayout(), repo.project), ComponentAware {

    override val project = repo.project
    private val issueList: JBList<Issue> = JBList()
    private val issueListModel: IssueListModel = IssueListModel()
    val renderer: IssueListCellRenderer

    init {
        val issueListScrollPane = JBScrollPane(issueList, VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_NEVER)
        renderer = IssueListCellRenderer({ issueListScrollPane.viewport.width }, IssueListCellIconProvider(project))
        issueList.cellRenderer = renderer
        add(issueListScrollPane, BorderLayout.CENTER)
        initIssueListModel()
        ListSpeedSearch(issueList)
    }

    private fun initIssueListModel() {
        issueList.emptyText.clear()
        issueList.model = issueListModel
        startLoading()
        if (issueStoreComponent[repo].getAllIssues().isEmpty()) {
            issueStoreComponent[repo].update(repo).doWhenDone {
                issueListModel.update()
                stopLoading()
            }
        } else {
            stopLoading()
        }
        // listen to IssueStore updates and repaint issue list accordingly
        issueUpdaterComponent.addUpdateListener {
            SwingUtilities.invokeLater {
                val placeholder = issueList.emptyText
                placeholder.clear()
                if (issueStoreComponent[repo].getAllIssues().isEmpty()) {
                    placeholder.appendText("No issues found ")
                    placeholder.appendText("Edit search request", SimpleTextAttributes.LINK_ATTRIBUTES,
                            { TaskManagerProxyComponent.CONFIGURE_SERVERS_ACTION_ID.runAction() })
                }
                issueListModel.update()
                val updatedSelectedIssueIndex = issueStoreComponent[repo].indexOf(getSelectedIssue())
                if (updatedSelectedIssueIndex == -1) {
                    issueList.clearSelection()
                } else {
                    issueList.selectedIndex = updatedSelectedIssueIndex
                }
                stopLoading()
            }
        }
    }

    fun getSelectedIssue() = when {
        issueList.selectedIndex == -1 -> null
        issueList.selectedIndex >= issueListModel.size -> null
        else -> issueListModel.getElementAt(issueList.selectedIndex)
    }

    fun getIssueCount() = issueListModel.size

    fun update() = issueListModel.update()

    fun addListSelectionListener(listener: () -> Unit) {
        issueList.addListSelectionListener { listener.invoke() }
    }

    override fun registerKeyboardAction(action: ActionListener, keyStroke: KeyStroke, condition: Int) {
        issueList.registerKeyboardAction(action, keyStroke, condition)
    }

    inner class IssueListModel : AbstractListModel<Issue>() {

        override fun getElementAt(index: Int) = issueStoreComponent[repo].getIssue(index)

        // we still can get this method invoked from swing focus lost handler on project close
        override fun getSize() = if (project.isDisposed) 0 else issueStoreComponent[repo].getAllIssues().size

        fun update() {
            fireContentsChanged(this, 0, size)
        }
    }
}