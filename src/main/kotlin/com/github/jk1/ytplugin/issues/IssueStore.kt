package com.github.jk1.ytplugin.issues

import com.github.jk1.ytplugin.issues.model.Issue
import com.github.jk1.ytplugin.logger
import com.github.jk1.ytplugin.rest.IssuesRestClient
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.util.ActionCallback

class IssueStore(val repo: YouTrackServer, private var issues: List<Issue> = listOf()) : Iterable<Issue> {
    private val client = IssuesRestClient(repo)
    private var currentCallback: ActionCallback = ActionCallback.Done()
    private val listeners: MutableSet<() -> Unit> = mutableSetOf()

    fun update(): ActionCallback {
        if (!isUpdating()) {
            logger.debug("Issue store refresh started for project ${repo.project.name} and YouTrack server ${repo.url}")
            currentCallback = ActionCallback()
            RefreshIssuesTask(currentCallback).queue()
        }
        return currentCallback
    }

    fun isUpdating() = !currentCallback.isDone

    fun getAllIssues(): Collection<Issue> = issues

    fun getIssue(index: Int) = issues[index]

    override fun iterator() = issues.iterator()

    fun addListener(listener: () -> Unit) {
        listeners.add(listener)
    }

    inner class RefreshIssuesTask(val future: ActionCallback) :
            Task.Backgroundable(repo.project, "Updating issues from server", true, ALWAYS_BACKGROUND) {

        override fun run(indicator: ProgressIndicator) {
            try {
                logger.debug("Fetching issues for search query: ${repo.defaultSearch}")
                repo.login()
                issues = client.getIssues(repo.defaultSearch)
            } catch (e: Exception) {
                logger.info("YouTrack issues refresh failed: ${e.message}")
                logger.debug(e)
                title = " Can't connect to YouTrack server. Are you offline?"
                Thread.sleep(5000) // display error message for a while
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
    }
}