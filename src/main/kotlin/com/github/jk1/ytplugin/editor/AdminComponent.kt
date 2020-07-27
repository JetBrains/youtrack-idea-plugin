package com.github.jk1.ytplugin.editor

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.editor.IssueNavigationLinkFactory.YouTrackIssueNavigationLink
import com.github.jk1.ytplugin.editor.IssueNavigationLinkFactory.createdByYouTrackPlugin
import com.github.jk1.ytplugin.editor.IssueNavigationLinkFactory.pointsTo
import com.github.jk1.ytplugin.editor.IssueNavigationLinkFactory.setProjects
import com.github.jk1.ytplugin.issues.model.Issue
import com.github.jk1.ytplugin.logger
import com.github.jk1.ytplugin.rest.AdminRestClient
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.intellij.concurrency.JobScheduler
import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.IssueNavigationConfiguration
import com.intellij.openapi.vcs.IssueNavigationLink
import com.intellij.util.concurrency.FutureResult
import java.util.concurrent.Future
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class AdminComponent(override val project: Project) : ProjectComponent, ComponentAware {

    companion object {
        val ALL_USERS = "All Users"
    }

    private lateinit var projectListRefreshTask: ScheduledFuture<*>

    fun getActiveTaskVisibilityGroups(issue: Issue, callback: (List<String>) -> Unit): Future<Unit> {
        val future = FutureResult<Unit>()
        object : Task.Backgroundable(project, "Loading eligible visibility groups") {
            override fun run(indicator: ProgressIndicator) {
                try {
                    indicator.text = title
                    val repo = taskManagerComponent.getYouTrackRepository(issue)
                    issue.entityId?.let { AdminRestClient(repo).getVisibilityGroups(it) }?.let { callback.invoke(it) }
                } catch (e: Throwable) {
                    logger.info("Failed to load eligible visibility groups for issue")
                    logger.debug(e)
                } finally {
                    future.set(Unit)
                }
            }
        }.queue()
        return future
    }

    override fun projectOpened() {
        // update navigation links every 30 min to recognize new projects
        projectListRefreshTask = JobScheduler.getScheduler().scheduleWithFixedDelay({
            updateNavigationLinkPatterns()
        }, 1, 60, TimeUnit.MINUTES)
        // update navigation links when server connection configuration has been changed
        taskManagerComponent.addConfigurationChangeListener { updateNavigationLinkPatterns() }
    }

    override fun projectClosed() {
        projectListRefreshTask.cancel(false)
    }

    private fun updateNavigationLinkPatterns() {
        val navigationConfig = IssueNavigationConfiguration.getInstance(project)
        navigationConfig.links.remove(null) // where are these nulls coming from I wonder
        taskManagerComponent.getAllConfiguredYouTrackRepositories().forEach { server ->
            val links = navigationConfig.links.filter { it.pointsTo(server) }
            val generatedLinks = links.filter { it.createdByYouTrackPlugin }
            if (links.isEmpty()) {
                // no issue links to that server have been defined so far
                val link = YouTrackIssueNavigationLink(server.url)
                updateIssueLinkProjects(link, server)
                navigationConfig.links.add(link)
            } else if (generatedLinks.isNotEmpty()) {
                // there is a link created by plugin, let's actualize it
                updateIssueLinkProjects(generatedLinks.first(), server)
            } else {
                logger.debug("Issue navigation link pattern for ${server.url} has been overridden and won't be updated")
            }
        }
    }

    private fun updateIssueLinkProjects(link: IssueNavigationLink, repo: YouTrackServer) {
        try {
            val projects = AdminRestClient(repo).getAccessibleProjects()
            if (projects.isEmpty()) {
                logger.debug("No accessible projects found for ${repo.url}")
            } else {
                link.setProjects(projects)
            }
        } catch (e: Exception) {
            logger.info(e)
        }
    }
}
