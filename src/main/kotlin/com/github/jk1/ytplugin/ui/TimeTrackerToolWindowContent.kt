package com.github.jk1.ytplugin.ui

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.issues.actions.*
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.github.jk1.ytplugin.timeTracker.actions.*
import com.intellij.openapi.project.Project
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

class TimeTrackerToolWindowContent(vertical: Boolean, val repo: YouTrackServer) : JPanel(BorderLayout()), ComponentAware {

    override val project: Project = repo.project

    private val splitter = EditorSplitter(vertical)
    private val workItemsList = WorkItemsList(repo)
    private val searchBar = WorkItemsSearchBar(repo)


    init {
        val leftPanel = JPanel(BorderLayout())
        leftPanel.add(searchBar, BorderLayout.NORTH)
        leftPanel.add(workItemsList, BorderLayout.CENTER)
        splitter.firstComponent = leftPanel
        add(splitter, BorderLayout.CENTER)
        add(createActionPanel(), BorderLayout.WEST)
        setupIssueListActionListeners()
    }

    private fun createActionPanel(): JComponent {
        val group = IssueActionGroup(this)
        group.add(RefreshWorkItemsAction(repo))
        group.add(ManualEntryAction())
        group.add(StartTrackerAction())
        group.add(ResetTrackerAction())
        group.add(PauseTrackerAction())
        group.add(StopTrackerAction())
        group.add(GroupByIssueAction())
        group.add(GroupByDateAction())
        group.addConfigureTaskServerAction(repo)
        group.add(HelpAction())
        return group.createVerticalToolbarComponent()
    }

    private fun setupIssueListActionListeners() {
        searchBar.actionListener = { search ->
            workItemsList.startLoading()
            issueWorkItemsStoreComponent[repo].filter(repo, search)
        }
    }
}