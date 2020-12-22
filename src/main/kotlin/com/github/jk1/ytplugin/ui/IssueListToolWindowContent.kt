package com.github.jk1.ytplugin.ui

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.issues.actions.*
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.intellij.openapi.project.Project
import java.awt.BorderLayout
import java.awt.event.KeyEvent.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.KeyStroke


class IssueListToolWindowContent(vertical: Boolean, val repo: YouTrackServer) : JPanel(BorderLayout()), ComponentAware {

    override val project: Project = repo.project

    private val splitter = EditorSplitter(vertical)
    private val viewer = IssueViewer()
    private val issuesList = IssueList(repo)
    private val searchBar = IssueSearchBar(repo)

    init {
        val leftPanel = JPanel(BorderLayout())
        leftPanel.add(searchBar, BorderLayout.NORTH)
        leftPanel.add(issuesList, BorderLayout.CENTER)
        splitter.firstComponent = leftPanel
        splitter.secondComponent = viewer
        add(splitter, BorderLayout.CENTER)
        add(createActionPanel(), BorderLayout.WEST)
        setupIssueListActionListeners()

    }

    private fun createActionPanel(): JComponent {
        val group = IssueActionGroup(this)
        val selectedIssue = { issuesList.getSelectedIssue() }
        group.add(RefreshIssuesAction(repo))
        group.add(CreateIssueAction())
        // todo: grouping and separators for actions
        group.add(AddCommentAction(selectedIssue))
        group.add(OpenCommandWindowAction(selectedIssue))
        group.add(SetAsActiveTaskAction(selectedIssue, repo))
        group.add(CopyIssueLinkAction(selectedIssue))
        group.add(BrowseIssueAction(selectedIssue))
        group.add(AnalyzeStacktraceAction(selectedIssue))
        group.add(PinIssueAction(selectedIssue))
        group.add(ToggleIssueViewAction(project, issuesList))
        group.addConfigureTaskServerAction(repo, false)
        group.add(HelpAction())
        return group.createVerticalToolbarComponent()
    }

    private fun setupIssueListActionListeners() {
        // update preview contents upon selection
        issuesList.addListSelectionListener {
            val selectedIssue = issuesList.getSelectedIssue()
            if (selectedIssue == null) {
                splitter.collapse()
            } else if (selectedIssue != viewer.currentIssue) {
                if (splitter.isCollapsedState()) {
                    splitter.expand()
                }
                viewer.showIssue(selectedIssue)
            }
        }

        issuesList.registerKeyboardAction({ splitter.collapse() }, KeyStroke.getKeyStroke(VK_RIGHT, 0), WHEN_FOCUSED)
        issuesList.registerKeyboardAction({ splitter.expand() }, KeyStroke.getKeyStroke(VK_LEFT, 0), WHEN_FOCUSED)
        issuesList.registerKeyboardAction({ splitter.expand() }, KeyStroke.getKeyStroke(VK_ENTER, 0), WHEN_FOCUSED)
        // expand issue preview on clickx-special/nautilus-clipboard
        //copy
        //file:///home/alina.boshchenko/WorkJB/intellij-community/java/idea-ui/src/com/intellij/jarRepository/RepositoryAttachDialog.form
        issuesList.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                if (issuesList.getIssueCount() > 0) {
                    splitter.expand()
                }
            }
        })
        // apply issue search
        searchBar.actionListener = { search ->
            issuesList.startLoading()
            repo.defaultSearch = search
            issueStoreComponent[repo].update(repo)
        }
    }
}