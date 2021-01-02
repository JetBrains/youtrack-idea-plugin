package com.github.jk1.ytplugin.ui

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.issues.model.IssueWorkItem
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.intellij.openapi.application.impl.ApplicationInfoImpl
import com.intellij.ui.ListSpeedSearch
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBLoadingPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBScrollPane.HORIZONTAL_SCROLLBAR_NEVER
import com.intellij.ui.components.JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
import com.intellij.util.ui.StatusText
import java.awt.BorderLayout
import java.awt.event.ActionListener
import javax.swing.AbstractListModel
import javax.swing.KeyStroke
import javax.swing.SwingUtilities


class WorkItemsList(val repo: YouTrackServer) : JBLoadingPanel(BorderLayout(), repo.project), ComponentAware {

    override val project = repo.project
    private val issueWorkItemListModel: IssueWorkItemsListModel = IssueWorkItemsListModel()
    private val issueWorkItemsList: JBList<IssueWorkItem> = JBList()
    private val renderer: WorkItemsListCellRenderer

    init {
        val issueWorkItemsListScrollPane = JBScrollPane(issueWorkItemsList, VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_NEVER)
        renderer = WorkItemsListCellRenderer({ issueWorkItemsListScrollPane.viewport.width }, repo.getRepo())
        issueWorkItemsList.cellRenderer = renderer
        add(issueWorkItemsListScrollPane, BorderLayout.CENTER)
        initIssueWorkItemsListModel()
        ListSpeedSearch(issueWorkItemsList)
    }

    fun getSelectedItem() = when {
        issueWorkItemsList.selectedIndex == -1 -> null
        issueWorkItemsList.selectedIndex >= issueWorkItemListModel.size -> null
        else -> issueWorkItemListModel.getElementAt(issueWorkItemsList.selectedIndex)
    }

    fun getIssuePosition(): List<Int> {
        return renderer.getIssuePosition()
    }

    private fun initIssueWorkItemsListModel() {
        issueWorkItemsList.emptyText.clear()
        issueWorkItemsList.model = issueWorkItemListModel
        startLoading()
        if (issueWorkItemsStoreComponent[repo].getAllWorkItems().isEmpty()) {
            issueWorkItemsStoreComponent[repo].update(repo).doWhenDone {
                issueWorkItemListModel.update()
                stopLoading()
            }
        } else {
            stopLoading()
        }
        // listen to IssueStore updates and repaint issueWorkItems list accordingly
        issueWorkItemsUpdaterComponent.subscribe {
            SwingUtilities.invokeLater {
                val placeholder = issueWorkItemsList.emptyText
                placeholder.clear()
                // use reflection to avoid IDE version compatibility issues
                if (issueWorkItemsStoreComponent[repo].getAllWorkItems().isEmpty()) {
                    placeholder.appendText("No work items found.")
                    if (ApplicationInfoImpl.getInstance().minorVersion.toInt() >= 2) {
                        StatusText::class.java.getMethod("appendLine",
                                String::class.java).invoke(placeholder, "Update your filter criteria and try again.")
                    } else {
                        placeholder.appendText(" Update your filter criteria and try again.")
                    }
                }

                issueWorkItemListModel.update()
                val updatedSelectedIssueWorkItemIndex = issueWorkItemsStoreComponent[repo].indexOf(getSelectedIssueWorkItem())
                if (updatedSelectedIssueWorkItemIndex == -1) {
                    issueWorkItemsList.clearSelection()
                } else {
                    issueWorkItemsList.selectedIndex = updatedSelectedIssueWorkItemIndex
                }
                stopLoading()
            }
        }
    }

    fun getSelectedIssueWorkItem() = when {
        issueWorkItemsList.selectedIndex == -1 -> null
        issueWorkItemsList.selectedIndex >= issueWorkItemListModel.size -> null
        else -> issueWorkItemListModel.getElementAt(issueWorkItemsList.selectedIndex)
    }

    fun getIssueWorkItemsCount() = issueWorkItemListModel.size

    fun update() = issueWorkItemListModel.update()

    fun addListSelectionListener(listener: () -> Unit) {
        issueWorkItemsList.addListSelectionListener { listener.invoke() }
    }

    override fun registerKeyboardAction(action: ActionListener, keyStroke: KeyStroke, condition: Int) {
        issueWorkItemsList.registerKeyboardAction(action, keyStroke, condition)
    }

    inner class IssueWorkItemsListModel : AbstractListModel<IssueWorkItem>() {

        override fun getElementAt(index: Int) = issueWorkItemsStoreComponent[repo].getWorkItem(index)

        override fun getSize() = if (project.isDisposed) 0 else issueWorkItemsStoreComponent[repo].getAllWorkItems().size

        fun update() {
            fireContentsChanged(this, 0, size)
        }
    }
}