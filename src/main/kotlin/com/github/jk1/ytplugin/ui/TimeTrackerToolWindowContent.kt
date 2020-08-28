package com.github.jk1.ytplugin.ui

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.issues.actions.*
import com.github.jk1.ytplugin.issues.model.IssueWorkItem
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.github.jk1.ytplugin.timeTracker.TimeTracker
import com.github.jk1.ytplugin.timeTracker.actions.*
import com.intellij.openapi.project.Project
import com.intellij.tasks.TaskManager
import java.awt.BorderLayout
import java.awt.event.KeyEvent.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.KeyStroke

class TimeTrackerToolWindowContent(vertical: Boolean, val repo: YouTrackServer) : JPanel(BorderLayout()), ComponentAware {

    override val project: Project = repo.project

    private val splitter = EditorSplitter(vertical)
    private val workItemsList = WorkItemsList(repo)
    private val timer = TimeTracker()
    private var taskManager = TaskManager.getManager(project)
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
        group.add(StartTrackerAction(repo, timer, project, taskManager))
        group.add(ResetTrackerAction(repo, timer, project, taskManager))
        group.add(PauseTrackerAction(timer))
        group.add(StopTrackerAction(timer, repo, project, taskManager))
        group.add(GroupByIssueAction(repo, workItemsList))
        group.add(GroupByDateAction(repo, workItemsList))
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