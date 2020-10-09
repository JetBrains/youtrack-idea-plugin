package com.github.jk1.ytplugin.timeTracker

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.logger
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.util.ActionCallback
import java.net.URL

class TimeTrackerStore(@Volatile private var trackerJson: String = "") {

    private var currentCallback: ActionCallback = ActionCallback.Done()

    fun update(repo: YouTrackServer): ActionCallback {
        if (!isUpdating()) {
            logger.debug("Time tracker refresh scheduled for project ${repo.project.name}")
            currentCallback = ActionCallback()
            RefreshIssuesTask(currentCallback, repo).queue()
        }
        return currentCallback
    }

    fun storeTime(repo: YouTrackServer) {
        RefreshIssuesTask(currentCallback, repo).fillTimeToStore(ComponentAware.of(repo.project).timeTrackerComponent)
    }

    private fun isUpdating() = !currentCallback.isDone

    fun getTrackerJson() = trackerJson


    inner class RefreshIssuesTask(private val future: ActionCallback, private val repo: YouTrackServer) :
            Task.Backgroundable(repo.project, "Updating time tracker", true, ALWAYS_BACKGROUND) {

        override fun run(indicator: ProgressIndicator) {
            try {
                logger.debug("Fetching time tracker for  ${repo.defaultSearch}")
                val timer = ComponentAware.of(project).timeTrackerComponent
                val store: PropertiesComponent = PropertiesComponent.getInstance(project)
                store.saveFields(timer)
                timer.activityTracker?.let { store.saveFields(it) }
            } catch (e: Exception) {
                displayErrorMessage("Error while loading time tracker", e)
            }
        }

        fun fillTimeToStore(timer: TimeTracker): String {
            val store: PropertiesComponent = PropertiesComponent.getInstance(project)
            store.saveFields(timer)
            store.setValue("inactivityPeriodInMills", timer.inactivityPeriodInMills.toString())
            logger.debug("Filled in time to store on project close")
            return trackerJson
        }

        private fun displayErrorMessage(message: String, exception: Exception) {
            logger.info("Time tracker refresh failed: ${exception.message}")
            logger.debug(exception)
            title = " $message"
            Thread.sleep(15000) // display error message for a while
        }

        override fun onCancel() {
            future.setDone()
            logger.debug("Time tracker refresh cancelled for YouTrack server ${repo.url}")
        }

        override fun onSuccess() {
            future.setDone()
            logger.debug("Time tracker has been updated for YouTrack server ${repo.url}")
            ComponentAware.of(repo.project).timeTrackerUpdaterComponent.onAfterUpdate()
        }
    }
}
