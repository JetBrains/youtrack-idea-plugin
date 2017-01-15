package com.github.jk1.ytplugin.ui

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.issues.actions.*
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import java.awt.BorderLayout
import java.awt.event.KeyEvent.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.KeyStroke

class IssueListToolWindowContent(val repo: YouTrackServer, parent: Disposable) : JPanel(BorderLayout()), ComponentAware {

    override val project: Project = repo.project

    private val splitter = EditorSplitter()
    private val viewer = IssueViewer(project)
    private val issueList = IssueList(repo, parent)
    private val searchBar = IssueSearchBar(repo)

    init {
        val leftPanel = JPanel(BorderLayout())
        leftPanel.add(searchBar, BorderLayout.NORTH)
        leftPanel.add(issueList, BorderLayout.CENTER)
        splitter.firstComponent = leftPanel
        splitter.secondComponent = viewer
        add(splitter, BorderLayout.CENTER)
        add(createActionPanel(), BorderLayout.WEST)
        setupIssueListActionListeners()
    }

    private fun createActionPanel(): JComponent {
        val group = IssueActionGroup(this)
        val selectedIssue = { issueList.getSelectedIssue() }
        group.add(RefreshIssuesAction(repo))
        //group.add(CreateIssueAction()) todo: implement me
        group.add(SetAsActiveTaskAction(selectedIssue, repo))
        group.add(CopyIssueLinkAction(selectedIssue))
        group.add(BrowseIssueAction(selectedIssue))
        group.add(AnalyzeStacktraceAction(selectedIssue))
        group.add(ToggleIssueViewAction(project, issueList))
        group.addConfigureTaskServerAction()
        return group.createVerticalToolbarComponent()
    }

    private fun setupIssueListActionListeners() {
        // update preview contents upon selection
        issueList.addListSelectionListener {
            val selectedIssue = issueList.getSelectedIssue()
            if (selectedIssue == null) {
                splitter.collapse()
            } else if (selectedIssue != viewer.currentIssue) {
                viewer.showIssue(selectedIssue)
            }
        }

        // keystrokes to expand/collapse issue preview
        issueList.registerKeyboardAction({ splitter.collapse() }, KeyStroke.getKeyStroke(VK_RIGHT, 0), WHEN_FOCUSED)
        issueList.registerKeyboardAction({ splitter.expand() }, KeyStroke.getKeyStroke(VK_LEFT, 0), WHEN_FOCUSED)
        issueList.registerKeyboardAction({ splitter.expand() }, KeyStroke.getKeyStroke(VK_ENTER, 0), WHEN_FOCUSED)
        // expand issue preview on click
        issueList.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                if (issueList.getIssueCount() > 0) {
                    splitter.expand()
                }
            }
        })
        // apply issue search
        searchBar.actionListener = { search ->
            issueList.startLoading()
            repo.defaultSearch = search
            issueStoreComponent[repo].update()
        }
    }
}