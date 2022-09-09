package com.github.jk1.ytplugin.timeTracker

import com.github.jk1.ytplugin.ComponentAware.Companion.of
import com.github.jk1.ytplugin.format
import com.github.jk1.ytplugin.logger
import com.github.jk1.ytplugin.rest.TimeTrackerRestClient
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import org.apache.http.HttpStatus
import org.apache.http.conn.HttpHostConnectException
import java.awt.Color
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

class TimeTrackerConnector(val repository: YouTrackServer, val project: Project) {

    fun postSavedWorkItemToServer(issueId: String, time: Long) {
        val timeTracker = of(project).timeTrackerComponent
        ApplicationManager.getApplication().executeOnPooledThread(
            Callable {
                postWorkItemToServer(
                    issueId, TimeTracker.formatTimePeriodToMinutes(time), timeTracker.type,
                    timeTracker.comment, getCurrentDate(), mapOf()
                )
            })
    }

    fun postWorkItemToServer(
        issueId: String, time: String, type: String,
        comment: String, date: String, attributes: Map<String, String>
    ) {

        logger.debug("Try posting work item for $issueId to ${repository.url}")
        val storage = of(project).spentTimePerTaskStorage
        val trackerNote = TrackerNotification()

        val postStatus = try {
            TimeTrackerRestClient(repository).postNewWorkItem(issueId, time, type, comment, date, attributes)
        } catch (e: HttpHostConnectException){
            trackerNote.notify("Connection to YouTrack server is lost, please check your network connection", NotificationType.WARNING)
            logger.warn("Connection to network lost: ${e.message}")
        } catch (e: IllegalArgumentException){
            logger.debug(e)
        }
        when (postStatus) {
            HttpStatus.SC_OK -> {
                trackerNote.notify(
                    "Spent time was successfully added for $issueId",
                    NotificationType.INFORMATION
                )

                of(project).issueWorkItemsStoreComponent[repository].update(repository)
                storage.resetSavedTimeForLocalTask(issueId)
            }
            HttpStatus.SC_FORBIDDEN -> {
                trackerNote.notify(
                    "Unable to post time to YouTrack. Please check if time tracking is enabled for this project. " +
                            "A record for $time min of tracked time has been saved locally.", NotificationType.WARNING
                )

                storage.resetSavedTimeForLocalTask(issueId)
                storage.setSavedTimeForLocalTask(issueId, TimeUnit.MINUTES.toMillis(time.toLong()))
            }
            HttpStatus.SC_BAD_REQUEST -> {
                trackerNote.notify(
                    "Please check if time tracking type $type belongs to the current project. " +
                            "A record for $time min of tracked time has been saved locally.", NotificationType.WARNING
                )
                storage.resetSavedTimeForLocalTask(issueId)
                storage.setSavedTimeForLocalTask(issueId, TimeUnit.MINUTES.toMillis(time.toLong()))
            }
            else -> {
                trackerNote.notify(
                    "Unable to post time to YouTrack. See IDE log for details. " +
                            "A record for $time min of tracked time has been saved locally.", NotificationType.WARNING
                )
                storage.resetSavedTimeForLocalTask(issueId)
                storage.setSavedTimeForLocalTask(issueId, TimeUnit.MINUTES.toMillis(time.toLong()))
            }
        }
    }

    fun postSavedWorkItemsToServer(savedItems: ConcurrentHashMap<String, Long>) {
        logger.debug("Try posting work item to ${repository.url}")

        val task = object : Task.Modal(project, "Post Time to YouTrack", true) {
            override fun run(indicator: ProgressIndicator) {
                indicator.text = "Post time tracking items to " + repository.url + "..."
                indicator.fraction = 0.0
                indicator.isIndeterminate = true

                val timeTracker = of(project).timeTrackerComponent

                savedItems.forEach { entry ->
                    ApplicationManager.getApplication().executeOnPooledThread(
                        Callable {
                            postWorkItemToServer(
                                entry.key, TimeTracker.formatTimePeriodToMinutes(entry.value), timeTracker.type,
                                timeTracker.comment, getCurrentDate(), mapOf()
                            )
                        })
                }
                of(project).issueWorkItemsStoreComponent[repository].update(repository)
            }
        }
        ProgressManager.getInstance().run(task)
    }

    fun addWorkItemManually(
        dateNotFormatted: String, selectedType: String, selectedId: String,
        comment: String, time: String,  attributes: Map<String, String>, notifier: JBLabel
    ): Future<Int> {
        val futureCode = ApplicationManager.getApplication().executeOnPooledThread(
            Callable {
                val sdf = SimpleDateFormat("dd MMM yyyy")
                val date = sdf.parse(dateNotFormatted)
                try {
                    TimeTrackerConnector(repository, repository.project).postWorkItemToServer(
                        selectedId, time, selectedType, comment,
                        date.time.toString(), attributes.filter { it.value.isNotEmpty() }
                    )
                    of(repository.project).issueWorkItemsStoreComponent[repository].update(repository)
                    HttpStatus.SC_OK
                } catch (e: IllegalStateException) {
                    logger.warn("Error in item type: ${e.message}")
                    notifier.foreground = Color.red
                    notifier.text = "Time could not be posted, please check your connection"
                    HttpStatus.SC_BAD_REQUEST
                } catch (e: Exception) {
                    logger.warn("Time was not posted. See IDE logs for details.")
                    logger.debug(e)
                    HttpStatus.SC_BAD_REQUEST
                }

            })
        return futureCode
    }

    private fun getCurrentDate(): String {
        val sdf = SimpleDateFormat("dd MMM yyyy")
        val date = sdf.parse(Date().format())
        return date.time.toString()
    }
}

