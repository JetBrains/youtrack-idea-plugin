package com.github.jk1.ytplugin.timeTracker

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.issues.model.Issue
import com.github.jk1.ytplugin.logger
import com.github.jk1.ytplugin.rest.IssuesRestClient
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.util.ActionCallback
import java.net.SocketTimeoutException

class TimeTrackerStore(@Volatile private var tracker: String = "a"){

    private var currentCallback: ActionCallback = ActionCallback.Done()

    fun update(repo: YouTrackServer): ActionCallback {
        if (!isUpdating()) {
            logger.debug("Issue store refresh scheduled for project ${repo.project.name} and YouTrack server ${repo.url}")
            currentCallback = ActionCallback()
            RefreshIssuesTask(currentCallback, repo).queue()
        }
        return currentCallback
    }

    fun isUpdating() = !currentCallback.isDone

    fun getTracker() = tracker


    inner class RefreshIssuesTask(private val future: ActionCallback, private val repo: YouTrackServer) :
            Task.Backgroundable(repo.project, "Updating issues from server", true, ALWAYS_BACKGROUND) {

        override fun run(indicator: ProgressIndicator) {
            try {
                logger.debug("Fetching issues for search query: ${repo.defaultSearch}")
                val timer  = ComponentAware.of(project).timeTrackerComponent
                tracker = timer.isAutoTrackingEnable.toString() + " " + timer.isManualTrackingEnable.toString() + " " +
                        timer.inactivityPeriodInMills + " " + timer.comment + " " +
                        timer.isScheduledUnabled.toString() + " " + timer.scheduledPeriod + " " +
                        timer.type + " " + timer.timeInMills + " " + timer.recordedTime + " " + timer.isRunning.toString()

            } catch (e: SocketTimeoutException) {
                displayErrorMessage("Failed to updated issues from YouTrack server. Request timed out.", e)
            } catch (e: Exception) {
                displayErrorMessage("Can't connect to YouTrack server. Are you offline?", e)
            }
        }

        private fun displayErrorMessage(message: String, exception: Exception){
            logger.info("YouTrack issues refresh failed: ${exception.message}")
            logger.debug(exception)
            title = " $message"
            Thread.sleep(15000) // display error message for a while
        }

        override fun onCancel() {
            future.setDone()
            logger.debug("Issue store refresh cancelled for YouTrack server ${repo.url}")
        }

        override fun onSuccess() {
            future.setDone()
            logger.debug("Issue store has been updated for YouTrack server ${repo.url}")
            ComponentAware.of(repo.project).timeTrackerUpdaterComponent.onAfterUpdate()
        }
    }
}