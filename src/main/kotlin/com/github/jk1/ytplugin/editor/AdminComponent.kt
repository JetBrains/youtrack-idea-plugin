package com.github.jk1.ytplugin.editor

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.logger
import com.github.jk1.ytplugin.rest.AdminRestClient
import com.intellij.concurrency.JobScheduler
import com.intellij.openapi.components.AbstractProjectComponent
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.IssueNavigationConfiguration
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class AdminComponent(override val project: Project) : AbstractProjectComponent(project), ComponentAware {

    private val restClient = AdminRestClient(project)
    private lateinit var projectListRefreshTask: ScheduledFuture<*>

    fun getActiveTaskVisibilityGroups(): List<String> {
        val repo = taskManagerComponent.getActiveYouTrackRepository()
        val taskId = taskManagerComponent.getActiveYouTrackTask().id
        return restClient.getVisibilityGroups(repo, taskId)
    }

    override fun projectOpened() {
        // update navigation links every 30 min to recognize new projects
        projectListRefreshTask = JobScheduler.getScheduler().scheduleWithFixedDelay({
            updateNavigationLinkPatterns()
        }, 0, 60, TimeUnit.MINUTES)
        // update navigation links when server connection configuration has been changed
        taskManagerComponent.addConfigurationChangeListener { updateNavigationLinkPatterns() }
    }

    override fun projectClosed() {
        projectListRefreshTask.cancel(false)
    }

    private fun updateNavigationLinkPatterns() {
        val navigationConfig = IssueNavigationConfiguration.getInstance(project)
        taskManagerComponent.getAllConfiguredYouTrackRepositories().forEach { repo ->
            try {
                val links = navigationConfig.links.filterNotNull().filter { link -> link.linkRegexp.startsWith(repo.url) }
                val generatedLinks = links.filter { it is YouTrackIssueNavigationLink }
                if (links.isEmpty() || generatedLinks.isNotEmpty()) {
                    repo.login()
                    val projects = restClient.getAccessibleProjects(repo)
                    if (projects.isNotEmpty()) {
                        var link = generatedLinks.firstOrNull() as? YouTrackIssueNavigationLink
                        if (link == null){
                            link = YouTrackIssueNavigationLink(repo.url)
                            navigationConfig.links.add(link)
                        }
                        link.setProjects(projects)
                    } else {
                        logger.info("No accessible projects found for ${repo.url}")
                    }
                } else {
                    logger.info("Issue navigation link pattern for ${repo.url} has been overridden and won't be updated")
                }
            } catch(e: Exception) {
                logger.error(e)
            }
        }
    }
}
