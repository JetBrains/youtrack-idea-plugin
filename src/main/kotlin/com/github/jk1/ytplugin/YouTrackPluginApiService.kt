package com.github.jk1.ytplugin

import com.github.jk1.ytplugin.commands.model.YouTrackCommandExecution
import com.github.jk1.ytplugin.issues.model.Issue
import com.github.jk1.ytplugin.rest.CommandRestClient
import com.github.jk1.ytplugin.rest.IssuesRestClient
import com.github.jk1.ytplugin.ui.IssueViewer
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.ContentFactory

@Service
class YouTrackPluginApiService(override val project: Project): YouTrackPluginApi, ComponentAware {

    override fun openIssueInToolWidow(issueId: String) {
        openIssueInToolWidow(findIssue(issueId))
    }

    override fun search(query: String): List<YouTrackIssue> {
        return taskManagerComponent.getAllConfiguredYouTrackRepositories().flatMap {
            IssuesRestClient(it).getIssues(query)
        }
    }

    override fun executeCommand(issue: YouTrackIssue, command: String): YouTrackCommandExecutionResult {
        if (issue !is Issue) {
            throw IllegalArgumentException("Can't handle issue that was not loaded from the plugin API")
        }
        val client = CommandRestClient(taskManagerComponent.getYouTrackRepository(issue))
        return client.executeCommand(YouTrackCommandExecution(issue, command, commentVisibleGroup = "All Users"))
    }

    fun openIssueInToolWidow(issue: Issue) {
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("YouTrack")!!
        val viewer = IssueViewer()
        val contentManager = toolWindow.contentManager
        val contentFactory = ContentFactory.SERVICE.getInstance()
        val content = contentFactory.createContent(viewer, issue.id, false)
        content.isCloseable = true
        contentManager.addContent(content)
        viewer.showIssue(issue)
        contentManager.setSelectedContent(content)
        toolWindow.show {
            contentManager.setSelectedContent(content)
            viewer.showIssue(issue)
        }
    }

    private fun findIssue(id: String): Issue {
        return taskManagerComponent.getAllConfiguredYouTrackRepositories()
                .map { issueStoreComponent[it] }
                .flatMap { it.getAllIssues() }
                .find { it.id == id } ?: findIssueOnServer(id)
    }

    private fun findIssueOnServer(id: String): Issue {
        for (server in taskManagerComponent.getAllConfiguredYouTrackRepositories()){
            try {
                return IssuesRestClient(server).getIssue(id)
            } catch (e: RuntimeException) {
                // most likely 404 from server
            }
        }
        throw IllegalArgumentException("No issue found for id $id")
    }
}