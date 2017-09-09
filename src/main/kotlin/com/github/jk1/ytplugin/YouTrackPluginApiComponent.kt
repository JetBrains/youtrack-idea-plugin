package com.github.jk1.ytplugin

import com.github.jk1.ytplugin.issues.model.Issue
import com.github.jk1.ytplugin.rest.IssuesRestClient
import com.github.jk1.ytplugin.ui.IssueViewer
import com.intellij.openapi.components.AbstractProjectComponent
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.ContentFactory

class YouTrackPluginApiComponent(override val project: Project) :
        AbstractProjectComponent(project), YouTrackPluginApi, ComponentAware {

    override fun openIssueInToolWidow(issueId: String) {
        openIssueInToolWidow(findIssue(issueId))
    }

    fun openIssueInToolWidow(issue: Issue) {
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("YouTrack")
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
                val issue = IssuesRestClient(server).getIssue(id)
                if (issue != null) {
                    return issue
                }
            } catch (e: RuntimeException) {
                // most likely 404 from server
            }
        }
        throw IllegalArgumentException("No issue found for id $id")
    }
}