package com.github.jk1.ytplugin.timeTracker

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.format
import com.github.jk1.ytplugin.issues.model.IssueWorkItem
import com.github.jk1.ytplugin.logger
import com.github.jk1.ytplugin.rest.UserRestClient
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.util.ActionCallback
import java.net.SocketTimeoutException

class IssueWorkItemStore(@Volatile private var workItems: List<IssueWorkItem> = listOf()) : Iterable<IssueWorkItem> {

    private var currentCallback: ActionCallback = ActionCallback.Done()
    var withGroupingByIssue = false
    var searchQuery = ""

    fun update(repo: YouTrackServer): ActionCallback {
        if (!isUpdating()) {
            logger.debug("Issue work item store refresh scheduled for project ${repo.project.name} and YouTrack server ${repo.url}")
            currentCallback = ActionCallback()
            RefreshIssuesWorkItemsTask(currentCallback, repo).queue()
        }
        return currentCallback
    }

    fun filter(repo: YouTrackServer, search: String): ActionCallback {
        if (!isUpdating()) {
            logger.debug("Issue work items filtering")
            currentCallback = ActionCallback()
            searchQuery = search
            RefreshIssuesWorkItemsTask(currentCallback, repo).queue()
        }
        return currentCallback
    }


    fun isUpdating() = !currentCallback.isDone

    fun getAllWorkItems() = workItems

    fun getWorkItem(index: Int) = workItems[index]

    override fun iterator() = workItems.iterator()

    inner class RefreshIssuesWorkItemsTask(private val future: ActionCallback, private val repo: YouTrackServer) :
            Task.Backgroundable(repo.project, "Updating work items from server", true, ALWAYS_BACKGROUND) {

        override fun run(indicator: ProgressIndicator) {
            try {
                logger.debug("Fetching issuesWorkItems for the search query")
                val list = UserRestClient(repo).getWorkItemsForUser()
                workItems = if (searchQuery != "")
                    filterWorkItems(searchQuery, list)
                else
                    list
                workItems = if (withGroupingByIssue)
                    workItems.sortedWith(compareBy { it.issueId })
                else workItems

            } catch (e: SocketTimeoutException) {
                displayErrorMessage("Failed to updated issueWorkItems from YouTrack server. Request timed out.", e)
            } catch (e: Exception) {
                displayErrorMessage("Can't connect to YouTrack server. Are you offline?", e)
            }
        }

        @OptIn(ExperimentalStdlibApi::class)
        private fun filterWorkItems(searchQuery: String, list: List<IssueWorkItem>): List<IssueWorkItem> {
            return list.filter {
                it.value.lowercase().contains(searchQuery.lowercase()) ||
                it.date.format().lowercase().contains(searchQuery.lowercase()) ||
                it.author.lowercase().contains(searchQuery.lowercase()) ||
                it.issueId.lowercase().contains(searchQuery.lowercase()) ||
                it.type.lowercase().contains(searchQuery.lowercase()) ||
                it.comment?.lowercase()?.contains(searchQuery.lowercase()) ?: false
            }
        }

        private fun displayErrorMessage(message: String, exception: Exception) {
            logger.info("YouTrack issueWorkItems refresh failed: ${exception.message}")
            logger.debug(exception)
            title = " $message"
            Thread.sleep(15000) // display error message for a while
        }

        override fun onCancel() {
            future.setDone()
            logger.debug("IssueWorkItems store refresh cancelled for YouTrack server ${repo.url}")
        }

        override fun onSuccess() {
            future.setDone()
            logger.debug("IssueWorkItems store has been updated for YouTrack server ${repo.url}")
            ComponentAware.of(repo.project).issueWorkItemsUpdaterComponent.onAfterUpdate()
        }
    }
}