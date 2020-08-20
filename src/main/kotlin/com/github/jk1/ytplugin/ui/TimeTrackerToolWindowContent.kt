package com.github.jk1.ytplugin.ui

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.issues.actions.*
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.github.jk1.ytplugin.timeTracker.TimeTracker
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
    private val viewer = IssueViewer()
    private val workItemsList = WorkItemsList(repo)
    private val timer = TimeTracker()
    private var taskManager = TaskManager.getManager(project)

    init {
        val leftPanel = JPanel(BorderLayout())
        leftPanel.add(workItemsList, BorderLayout.CENTER)
        splitter.firstComponent = leftPanel
        splitter.secondComponent = viewer
        add(splitter, BorderLayout.CENTER)
        add(createActionPanel(), BorderLayout.WEST)
        setupIssueWorkItemsListActionListeners()
    }

    private fun createActionPanel(): JComponent {
        val group = IssueActionGroup(this)
        group.add(RefreshWorkItemsAction(repo))

        group.add(StartTrackerAction(repo, timer, project, taskManager))
        group.add(StopTrackerAction(timer, repo, project))

        group.add(CreateIssueAction())
        group.addConfigureTaskServerAction(repo)
        group.add(HelpAction())
        return group.createVerticalToolbarComponent()
    }

    private fun setupIssueWorkItemsListActionListeners() {
        // update preview contents upon selection
        workItemsList.addListSelectionListener {
            val selectedIssueWorkItem = workItemsList.getSelectedIssueWorkItem()
            if (selectedIssueWorkItem == null) {
                splitter.collapse()
            } else {
                viewer.showWorkItems(selectedIssueWorkItem)
            }
        }

        // keystrokes to expand/collapse preview
        workItemsList.registerKeyboardAction({ splitter.collapse() }, KeyStroke.getKeyStroke(VK_RIGHT, 0), WHEN_FOCUSED)
        workItemsList.registerKeyboardAction({ splitter.expand() }, KeyStroke.getKeyStroke(VK_LEFT, 0), WHEN_FOCUSED)
        workItemsList.registerKeyboardAction({ splitter.expand() }, KeyStroke.getKeyStroke(VK_ENTER, 0), WHEN_FOCUSED)
        // expand  preview on click
        workItemsList.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                if (workItemsList.getIssueWorkItemsCount() > 0) {
                    splitter.expand()
                }
            }
        })
    }
}