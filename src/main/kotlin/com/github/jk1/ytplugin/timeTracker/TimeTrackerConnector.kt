package com.github.jk1.ytplugin.timeTracker

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.logger
import com.github.jk1.ytplugin.rest.MulticatchException.Companion.multicatchException
import com.github.jk1.ytplugin.rest.TimeTrackerRestClient
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.*
import java.util.concurrent.Callable

class TimeTrackerConnector {

    fun postSavedTimeToServer(repository: YouTrackServer, project: Project) {

        logger.debug("Try posting work item to ${repository.url}")

        val future = ApplicationManager.getApplication().executeOnPooledThread(
            Callable {
                try {
                    val store = ComponentAware.of(project).spentTimePerTaskStorage
                    val savedItems = store.getAllStoredItems()
                    val timeTracker = ComponentAware.of(project).timeTrackerComponent

                    savedItems.forEach { entry ->
                        logger.debug("Try posting work item for ${entry.key} to ${repository.url}")

                        TimeTrackerRestClient(repository).postNewWorkItem(
                            entry.key, TimeTracker.formatTimePeriod(entry.value),
                            timeTracker.type, timeTracker.comment, (Date().time).toString()
                        )
                        store.resetSavedTimeForLocalTask(entry.key)
                    }

                    ComponentAware.of(project).issueWorkItemsStoreComponent[repository].update(repository)
                } catch (e: Exception) {
                    e.multicatchException(
                        SocketException::class.java,
                        UnknownHostException::class.java,
                        SocketTimeoutException::class.java
                    ) {
                        logger.warn("Exception in time tracker: ${e.message}")
                    }
                }
            })
        future.get()

    }
}

