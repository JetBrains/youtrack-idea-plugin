package com.github.jk1.ytplugin.timeTracker

import com.github.jk1.ytplugin.ComponentAware.Companion.of
import com.github.jk1.ytplugin.logger
import com.github.jk1.ytplugin.rest.TimeTrackerRestClient
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.intellij.notification.NotificationType
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import org.apache.http.HttpStatus
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class TimeTrackerConnector(val repository: YouTrackServer, val project: Project) {

    fun postSavedWorkItemToServer(issueId: String, time: Long) {
        val timeTracker = of(project).timeTrackerComponent
        postWorkItemToServer(issueId, TimeTracker.formatTimePeriod(time),  timeTracker.type,
            timeTracker.comment, (Date().time).toString())
    }

    fun postWorkItemToServer(issueId: String, time: String, type: String,
                             comment: String, date: String) {
        logger.debug("Try posting work item for $issueId to ${repository.url}")
        val storage = of(project).spentTimePerTaskStorage

        val postStatus = TimeTrackerRestClient(repository).postNewWorkItem(issueId, time, type, comment, date)
        if (postStatus == HttpStatus.SC_OK){
            storage.resetSavedTimeForLocalTask(issueId)
        } else {
            val trackerNote = TrackerNotification()
            trackerNote.notify("Unable to post time to YouTrack. See IDE log for details. " +
                    "Time $time min is saved",
                NotificationType.WARNING)

            storage.resetSavedTimeForLocalTask(issueId)
            storage.setSavedTimeForLocalTask(issueId, TimeUnit.MINUTES.toMillis(time.toLong()))
        }
    }

    fun postSavedWorkItemsToServer(savedItems: ConcurrentHashMap<String, Long>) {
        logger.debug("Try posting work item to ${repository.url}")

        val task = object : Task.Modal(project, "Post time to YouTrack", true) {
            override fun run(indicator: ProgressIndicator) {
                indicator.text = "Post time tracking items to " + repository.url + "..."
                indicator.fraction = 0.0
                indicator.isIndeterminate = true

                val timeTracker = of(project).timeTrackerComponent

                savedItems.forEach { entry ->
                    postWorkItemToServer(entry.key, TimeTracker.formatTimePeriod(entry.value), timeTracker.type,
                        timeTracker.comment, (Date().time).toString())
                }
                of(project).issueWorkItemsStoreComponent[repository].update(repository)
            }
        }
        ProgressManager.getInstance().run(task)
    }

}

