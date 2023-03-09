package com.github.jk1.ytplugin.ui

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.issues.model.Issue
import com.github.jk1.ytplugin.setup.SetupDialog
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.intellij.ui.ListSpeedSearch
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBLoadingPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBScrollPane.HORIZONTAL_SCROLLBAR_NEVER
import com.intellij.ui.components.JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
import java.awt.BorderLayout
import java.awt.event.ActionEvent
import java.awt.event.MouseListener
import javax.swing.AbstractAction
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
        ListSpeedSearch.installOn(issueList)
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
        issueUpdaterComponent.subscribe {
            SwingUtilities.invokeLater {
                val placeholder = issueList.emptyText
                placeholder.clear()
                if (issueStoreComponent[repo].getAllIssues().isEmpty()) {
                    placeholder.appendText("No issues found. Edit search request or ")
                    placeholder.appendText("configuration", SimpleTextAttributes.LINK_ATTRIBUTES
                    ) { SetupDialog(project, repo, false).show() }
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

    fun getIssueById(id: String) = when {
        issueList.selectedIndex == -1 -> null
        issueList.selectedIndex >= issueListModel.size -> null
        else -> issueListModel.getElementById(id)
    }

    fun setSelectedIssue(issue: Issue) {
       issueList.selectedIndex = issueListModel.getIndexOfElement(issue)
    }

    fun getIssueCount() = issueListModel.size

    fun update() = issueListModel.update()

    fun addListSelectionListener(listener: () -> Unit) {
        issueList.addListSelectionListener { listener.invoke() }
    }

    fun addComponentInput(key: String, keyStroke: KeyStroke) {
        issueList.getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(keyStroke, key)
    }

    fun addComponentAction(key: String, action: () -> Unit) {
        issueList.actionMap.put(key, object: AbstractAction(){
            override fun actionPerformed(e: ActionEvent) {
                action.invoke()
            }
        })
    }

    override fun addMouseListener(l: MouseListener) {
        issueList.addMouseListener(l)
    }

    inner class IssueListModel : AbstractListModel<Issue>() {

        override fun getElementAt(index: Int) = issueStoreComponent[repo].getIssue(index)

        fun getIndexOfElement(issue: Issue) = issueStoreComponent[repo].getIndex(issue)

        fun getElementById(id: String) = issueStoreComponent[repo].getIssueById(id)


        // we still can get this method invoked from swing focus lost handler on project close
        override fun getSize() = if (project.isDisposed) 0 else issueStoreComponent[repo].getAllIssues().size

        fun update() {
            fireContentsChanged(this, 0, size)
        }
    }
}