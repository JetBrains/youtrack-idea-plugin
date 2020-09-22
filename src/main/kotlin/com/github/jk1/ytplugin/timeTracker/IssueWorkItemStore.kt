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
    var withGrouping = false
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
            Task.Backgroundable(repo.project, "Updating issuesWorkItems from server", true, ALWAYS_BACKGROUND) {

        override fun run(indicator: ProgressIndicator) {
            try {
                logger.debug("Fetching issuesWorkItems for the search query")
                val search = repo.defaultSearch + "#{Assigned to me}"
                val list = UserRestClient(repo).getWorkItemsForUser(search)
                workItems = if (searchQuery != "")
                    filterWorkItems(searchQuery, list)
                else
                    list
                workItems = if (withGrouping)
                    workItems.sortedWith(compareBy { it.issueId })
                else workItems
            } catch (e: SocketTimeoutException) {
                displayErrorMessage("Failed to updated issueWorkItems from YouTrack server. Request timed out.", e)
            } catch (e: Exception) {
                displayErrorMessage("Can't connect to YouTrack server. Are you offline?", e)
            }
        }

        private fun filterWorkItems(searchQuery: String, list: List<IssueWorkItem>) : List<IssueWorkItem>{
            return list.filter {
                it.value.contains(searchQuery) ||
                        it.author.contains(searchQuery) ||
                        it.issueId.contains(searchQuery) ||
                        it.date.format().contains(searchQuery) ||
                        it.type.contains(searchQuery)
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