package com.github.jk1.ytplugin.ui

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.issues.actions.*
import com.github.jk1.ytplugin.issues.model.Issue
import com.github.jk1.ytplugin.runAction
import com.github.jk1.ytplugin.tasks.TaskManagerProxyComponent.Companion.CONFIGURE_SERVERS_ACTION_ID
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.ui.ListSpeedSearch
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBLoadingPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBScrollPane.HORIZONTAL_SCROLLBAR_NEVER
import com.intellij.ui.components.JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
import java.awt.BorderLayout
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.AbstractListModel
import javax.swing.JComponent
import javax.swing.KeyStroke
import javax.swing.SwingUtilities

class IssueListToolWindowContent(override val project: Project, val repo: YouTrackServer, parent: Disposable) :
        JBLoadingPanel(BorderLayout(), parent), ComponentAware {

    private val issueList: JBList = JBList()
    private val splitter = EditorSplitter()
    private val viewer = IssueViewer(project)
    private val issueListModel: IssueListModel = IssueListModel()
    private lateinit var issueCellRenderer: IssueListCellRenderer

    init {
        val issueListScrollPane = JBScrollPane(issueList, VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_NEVER)
        issueCellRenderer = IssueListCellRenderer({issueListScrollPane.viewport.width})
        issueList.cellRenderer = issueCellRenderer
        splitter.firstComponent = issueListScrollPane
        splitter.secondComponent = viewer
        add(splitter, BorderLayout.CENTER)
        add(createActionPanel(), BorderLayout.WEST)
        setupIssueListActionListeners()
        initIssueListModel()
        ListSpeedSearch(issueList)
    }

    private fun createActionPanel(): JComponent {
        val group = DefaultActionGroup()
        val selectedIssue = {
            when {
                issueList.selectedIndex == -1 -> null
                else -> issueListModel.getElementAt(issueList.selectedIndex)
            }
        }
        group.add(RefreshIssuesAction(repo))
        //group.add(CreateIssueAction()) todo: implement me
        group.add(SetAsActiveTaskAction(selectedIssue, repo))
        group.add(BrowseIssueAction(selectedIssue))
        group.add(AnalyzeStacktraceAction(selectedIssue))
        group.add(ToggleIssueViewAction(project, issueCellRenderer, issueListModel))
        group.add(ActionManager.getInstance().getAction(CONFIGURE_SERVERS_ACTION_ID))
        return ActionManager.getInstance()
                .createActionToolbar("Actions", group, false)
                .component
    }

    private fun setupIssueListActionListeners(){
        // update preview contents upon selection
        issueList.addListSelectionListener {
            val selectedIndex = issueList.selectedIndex
            if (selectedIndex == -1) {
                splitter.collapse()
            } else {
                val issue = issueListModel.getElementAt(selectedIndex)
                if (!issue.equals(viewer.currentIssue)) {
                    viewer.showIssue(issue)
                }
            }
        }
        // keystrokes to expand/collapse issue preview
        issueList.registerKeyboardAction({ splitter.collapse() },
                KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), JComponent.WHEN_FOCUSED)
        issueList.registerKeyboardAction({ splitter.expand() },
                KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), JComponent.WHEN_FOCUSED)
        issueList.registerKeyboardAction({ splitter.expand() },
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), JComponent.WHEN_FOCUSED)
        // expand issue preview on click
        issueList.addMouseListener(object: MouseAdapter(){
            override fun mousePressed(e: MouseEvent) {
                if (issueList.model.size > 0) {
                    splitter.expand()
                }
            }
        })
    }

    private fun initIssueListModel() {
        issueList.emptyText.clear()
        issueList.model = issueListModel
        startLoading()
        if (issueStoreComponent[repo].getAllIssues().isEmpty()) {
            issueStoreComponent[repo].update().doWhenDone {
                issueListModel.update()
                stopLoading()
            }
        } else {
            stopLoading()
        }
        // listen to IssueStore updates and repaint issue list accordingly
        issueStoreComponent[repo].addListener {
            SwingUtilities.invokeLater {
                val placeholder = issueList.emptyText
                placeholder.clear()
                if (issueStoreComponent[repo].getAllIssues().isEmpty()) {
                    placeholder.appendText("No issues found ")
                    placeholder.appendText("Edit search request", SimpleTextAttributes.LINK_ATTRIBUTES,
                            { CONFIGURE_SERVERS_ACTION_ID.runAction() })
                }
                issueListModel.update()
                val updatedSelectedIssueIndex = issueStoreComponent[repo].indexOf(viewer.currentIssue)
                if (updatedSelectedIssueIndex == -1) {
                    issueList.clearSelection()
                } else {
                    issueList.selectedIndex = updatedSelectedIssueIndex
                }
            }
        }
    }

    inner class IssueListModel: AbstractListModel<Issue>() {

        override fun getElementAt(index: Int) = issueStoreComponent[repo].getIssue(index)

        override fun getSize() = issueStoreComponent[repo].getAllIssues().size

        fun update() { fireContentsChanged(IssueListPanel@this, 0, size) }
    }
}