package com.github.jk1.ytplugin.search

import com.github.jk1.ytplugin.search.actions.CreateIssueAction
import com.github.jk1.ytplugin.search.actions.RefreshIssuesAction
import com.github.jk1.ytplugin.search.actions.SetAsActiveTaskAction
import com.github.jk1.ytplugin.search.model.Issue
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.ui.CollectionListModel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBLoadingPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBScrollPane.HORIZONTAL_SCROLLBAR_NEVER
import com.intellij.ui.components.JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
import java.awt.BorderLayout
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.JComponent

class IssueViewer(val project: Project, parent: Disposable) : JBLoadingPanel(BorderLayout(), parent), DataProvider {

    var issueList: JBList = JBList()

    init {
        val splitter = EditorSplitter(project)
        val browser = MessageBrowser(project)
        issueList.fixedCellHeight = 80
        issueList.cellRenderer = IssueListCellRenderer()
        issueList.model = CollectionListModel<Issue>()
        issueList.addListSelectionListener {
            browser.showIssue(issueList.model.getElementAt(it.firstIndex) as Issue)
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

    private fun createActionPanel(): JComponent{
        val group = DefaultActionGroup()
        group.add(RefreshIssuesAction())
        group.add(CreateIssueAction())
        group.add(SetAsActiveTaskAction())
        return ActionManager.getInstance()
                .createActionToolbar("Actions", group, false)
                .component
    }

    private fun initModel(){
        startLoading()
        ApplicationManager.getApplication().executeOnPooledThread {
            var model = issueList.model
            try {
                model = IssuesModel(project)
            } finally {
                ApplicationManager.getApplication().invokeLater {
                    issueList.model = model
                    stopLoading()
                }
            }
        }
    }

    override fun getData(dataId: String): Any? {
        if (PlatformDataKeys.PROJECT.equals(dataId)) {
            return project
        }
        if (DataKey.create<Array<Issue>>("MYY_ISSUES_ARRAY").equals(dataId)) {
            val values = issueList.selectedValuesList
            val issues = arrayOfNulls<Issue>(values.size)
            for (i in values.indices) {
                issues[i] = values[i] as Issue
            }
            return issues
        }
        return null
    }
}