package com.github.jk1.ytplugin.ui

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.issues.actions.HelpAction
import com.github.jk1.ytplugin.issues.actions.IssueActionGroup
import com.github.jk1.ytplugin.logger
import com.github.jk1.ytplugin.rest.MulticatchException.Companion.multicatchException
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.github.jk1.ytplugin.timeTracker.actions.*
import com.intellij.openapi.project.Project
import java.awt.BorderLayout
import java.awt.Desktop
import java.io.IOException
import java.net.*
import javax.swing.JComponent
import javax.swing.JPanel


class TimeTrackerToolWindowContent(vertical: Boolean, val repo: YouTrackServer) : JPanel(BorderLayout()), ComponentAware {

    override val project: Project = repo.project

    private val splitter = EditorSplitter(vertical)
    private val workItemsList = WorkItemsList(repo)
    private val searchBar = WorkItemsSearchBar(repo)

    // flag is required to avoid multiple windows opening
    var isOpenedOnce = true

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
        group.add(PauseTrackerAction())
        group.add(ResetTrackerAction())
        group.add(StopTrackerAction())
        group.add(ToggleGroupByAction(repo))
        group.addConfigureTaskServerAction(repo, true)
        group.add(HelpAction())
        return group.createVerticalToolbarComponent()
    }

    private fun setupIssueListActionListeners() {

        workItemsList.addListSelectionListener {
            val selectedItem = workItemsList.getSelectedItem()
            if (isOpenedOnce) {
                if (selectedItem != null) {
                    val issueIdStart = workItemsList.getIssuePosition()[0]
                    val issueIdEnd = issueIdStart + workItemsList.getIssuePosition()[1]
                    if (mousePosition.x in issueIdStart until issueIdEnd) {
                        try {
                            Desktop.getDesktop().browse(URI("${repo.url}/issue/${selectedItem.issueId}"))
                        } catch (e: Exception) {
                            e.multicatchException(IOException::class, URISyntaxException::class) {
                                logger.debug("Error in issue opening in the browser: ${e.message}")
                            }
                        }
                    }
                }
                isOpenedOnce = !isOpenedOnce
            } else {
                isOpenedOnce = !isOpenedOnce
            }
        }
        searchBar.actionListener = { search ->
            workItemsList.startLoading()
            issueWorkItemsStoreComponent[repo].filter(repo, search)
        }
    }
}