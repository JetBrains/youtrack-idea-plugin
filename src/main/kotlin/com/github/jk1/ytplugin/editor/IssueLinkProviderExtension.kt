package com.github.jk1.ytplugin.editor

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.editor.IssueNavigationLinkFactory.YouTrackIssueNavigationLink
import com.github.jk1.ytplugin.editor.IssueNavigationLinkFactory.createdByYouTrackPlugin
import com.github.jk1.ytplugin.editor.IssueNavigationLinkFactory.pointsTo
import com.github.jk1.ytplugin.editor.IssueNavigationLinkFactory.setProjects
import com.github.jk1.ytplugin.logger
import com.github.jk1.ytplugin.rest.AdminRestClient
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.intellij.concurrency.JobScheduler
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vcs.IssueNavigationConfiguration
import com.intellij.openapi.vcs.IssueNavigationLink
import java.util.concurrent.TimeUnit

class IssueLinkProviderExtension : StartupActivity.Background {

    companion object {
        const val ALL_USERS = "All Users"
    }

    override fun runActivity(project: Project) {
        // todo: https://github.com/jk1/youtrack-idea-plugin/issues/105
        // update navigation links every 30 min to recognize new projects
        val projectListRefreshTask = JobScheduler.getScheduler().scheduleWithFixedDelay({
            updateNavigationLinkPatterns(project)
        }, 1, 60, TimeUnit.MINUTES)
        // update navigation links when server connection configuration has been changed
        ComponentAware.of(project).taskManagerComponent.addConfigurationChangeListener {
            updateNavigationLinkPatterns(project)
        }
        Disposer.register(ComponentAware.of(project).sourceNavigatorComponent, // any project-level disposable will do
                Disposable { projectListRefreshTask.cancel(false) })
    }

    private fun updateNavigationLinkPatterns(project: Project) {
        val navigationConfig = IssueNavigationConfiguration.getInstance(project)
        navigationConfig.links.remove(null) // where are these nulls coming from I wonder
        ComponentAware.of(project).taskManagerComponent.getAllConfiguredYouTrackRepositories().forEach { server ->
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
