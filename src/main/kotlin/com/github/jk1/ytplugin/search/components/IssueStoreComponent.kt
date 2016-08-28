package com.github.jk1.ytplugin.search.components

import com.github.jk1.ytplugin.common.YouTrackServer
import com.github.jk1.ytplugin.common.logger
import com.github.jk1.ytplugin.search.model.Issue
import com.github.jk1.ytplugin.search.rest.IssuesRestClient
import com.intellij.concurrency.JobScheduler
import com.intellij.openapi.components.AbstractProjectComponent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ActionCallback
import java.io.Closeable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.swing.SwingUtilities

class IssueStoreComponent(val project: Project) : AbstractProjectComponent(project) {

    private val stores = ConcurrentHashMap<YouTrackServer, Store>()

    operator fun get(repo: YouTrackServer): Store {
        return stores.getOrPut(repo, {
            logger.debug("Issue store opened for project ${project.name} and YouTrack server ${repo.url}")
            Store(repo)
        })
    }

    override fun projectClosed() {
        stores.values.forEach { it.close() }
    }

    inner class Store(private val repo: YouTrackServer) : Closeable {
        private val client = IssuesRestClient(project, repo)
        private var issues: List<Issue> = listOf()
        private var currentCallback: ActionCallback = ActionCallback.Done()
        private val listeners = mutableSetOf({
            /** todo: fileStore().save() */
        })
        private val timedRefreshTask = JobScheduler.getScheduler().scheduleWithFixedDelay({
            SwingUtilities.invokeLater { update() }
        }, 5, 5, TimeUnit.MINUTES)             //  todo: customizable update interval

        override fun close() {
            timedRefreshTask.cancel(false)
        }

        fun update(): ActionCallback {
            if (isUpdating()) {
                return currentCallback
            }
            currentCallback = refresh()
            return currentCallback
        }

        private fun refresh(): ActionCallback {
            logger.debug("Issue store refresh started for project ${project.name} and YouTrack server ${repo.url}")
            val future = ActionCallback()
            object : Task.Backgroundable(project, "Updating issues from server", true, ALWAYS_BACKGROUND) {
                override fun run(indicator: ProgressIndicator) {
                    try {
                        logger.debug("Fetching issues for search query: ${repo.defaultSearch}")
                        repo.login()
                        issues = client.getIssues(repo.defaultSearch)
                    } catch (e: Exception) {
                        // todo: notification?
                        logger.error("YouTrack issues refresh failed", e)
                    }
                }

                override fun onCancel() {
                    future.setDone()
                    logger.debug("Issue store refresh cancelled for YouTrack server ${repo.url}")
                }

                override fun onSuccess() {
                    future.setDone()
                    logger.debug("Issue store has been updated for YouTrack server ${repo.url}")
                    listeners.forEach { it.invoke() }
                }
            }.queue()
            return future
        }


        fun isUpdating() = !currentCallback.isDone

        fun getAllIssues(): Collection<Issue> = issues

        fun getIssue(index: Int) = issues[index]

        fun addListener(listener: () -> Unit) {
            listeners.add(listener)
        }
    }
}