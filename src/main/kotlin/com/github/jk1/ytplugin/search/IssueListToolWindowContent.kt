package com.github.jk1.ytplugin.search

import com.github.jk1.ytplugin.common.YouTrackServer
import com.github.jk1.ytplugin.common.components.ComponentAware
import com.github.jk1.ytplugin.common.components.TaskManagerProxyComponent.Companion.CONFIGURE_SERVERS_ACTION_ID
import com.github.jk1.ytplugin.common.runAction
import com.github.jk1.ytplugin.search.actions.BrowseIssueAction
import com.github.jk1.ytplugin.search.actions.RefreshIssuesAction
import com.github.jk1.ytplugin.search.actions.SetAsActiveTaskAction
import com.github.jk1.ytplugin.search.model.Issue
import com.intellij.ide.DataManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.ui.CollectionListModel
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBLoadingPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBScrollPane.HORIZONTAL_SCROLLBAR_NEVER
import com.intellij.ui.components.JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
import java.awt.BorderLayout
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.AbstractListModel
import javax.swing.JComponent

class IssueListToolWindowContent(override val project: Project, val repo: YouTrackServer, parent: Disposable) :
        JBLoadingPanel(BorderLayout(), parent), ComponentAware {

    private var issueList: JBList = JBList()
    private lateinit var issueListModel: AbstractListModel<Issue>

    init {
        val splitter = EditorSplitter(project)
        val browser = IssueViewer(project)
        issueList.cellRenderer = IssueListCellRenderer()
        issueList.model = CollectionListModel<Issue>()
        issueList.addListSelectionListener {
            val issue = issueListModel.getElementAt(issueList.selectedIndex)
            if (!issue.equals(browser.currentIssue)) {
                browser.showIssue(issue)
            }
        }
        val scrollPane = JBScrollPane(issueList, VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_NEVER)
        scrollPane.addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent) {
                issueList.fixedCellWidth = scrollPane.visibleRect.width - 30
            }
        })
        splitter.firstComponent = scrollPane
        splitter.secondComponent = browser
        add(splitter, BorderLayout.CENTER)
        add(createActionPanel(), BorderLayout.WEST)

        initModel()
    }

    private fun createActionPanel(): JComponent {
        val group = DefaultActionGroup()
        val selectedTask = {
            when {
                issueList.selectedIndex == -1 -> null
                else -> issueListModel.getElementAt(issueList.selectedIndex).asTask()
            }
        }
        group.add(RefreshIssuesAction(repo))
        //group.add(CreateIssueAction()) todo: implement me
        group.add(SetAsActiveTaskAction(selectedTask))
        group.add(BrowseIssueAction(selectedTask))
        group.add(ActionManager.getInstance().getAction(CONFIGURE_SERVERS_ACTION_ID))
        return ActionManager.getInstance()
                .createActionToolbar("Actions", group, false)
                .component
    }

    private fun initModel() {
        issueList.emptyText.clear()
        startLoading()
        issueListModel = object : AbstractListModel<Issue>() {
            init {
                issueStoreComponent[repo].addListener { fireContentsChanged(IssueListPanel@this, 0, size) }
            }

            override fun getElementAt(index: Int) = issueStoreComponent[repo].getIssue(index)

            override fun getSize() = issueStoreComponent[repo].getAllIssues().size
        }
        issueList.model = issueListModel
        issueStoreComponent[repo].update().doWhenDone { stopLoading() }
        issueStoreComponent[repo].addListener {
            val placeholder = issueList.emptyText
            placeholder.clear()
            if (issueStoreComponent[repo].getAllIssues().isEmpty()) {
                placeholder.appendText("No issues found ")
                placeholder.appendText("Edit search request", SimpleTextAttributes.LINK_ATTRIBUTES,
                        { CONFIGURE_SERVERS_ACTION_ID.runAction() })
            }
        }
    }
}