package com.github.jk1.ytplugin.search

import com.github.jk1.ytplugin.common.components.ComponentAware
import com.github.jk1.ytplugin.search.actions.BrowseIssueAction
import com.github.jk1.ytplugin.search.actions.CreateIssueAction
import com.github.jk1.ytplugin.search.actions.RefreshIssuesAction
import com.github.jk1.ytplugin.search.actions.SetAsActiveTaskAction
import com.github.jk1.ytplugin.search.model.Issue
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.tasks.impl.BaseRepository
import com.intellij.ui.CollectionListModel
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

class IssueListPanel(override val project: Project, val repo: BaseRepository, parent: Disposable) :
        JBLoadingPanel(BorderLayout(), parent), ComponentAware {

    private var issueList: JBList = JBList()
    private lateinit var issueListModel: AbstractListModel<Issue>

    init {
        val splitter = EditorSplitter(project)
        val browser = IssueBrowserPanel(project)
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
        group.add(CreateIssueAction())
        group.add(SetAsActiveTaskAction(selectedTask))
        group.add(BrowseIssueAction(selectedTask))
        return ActionManager.getInstance()
                .createActionToolbar("Actions", group, false)
                .component
    }

    private fun initModel() {
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
    }

}