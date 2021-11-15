package com.github.jk1.ytplugin.timeTracker

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.logger
import com.github.jk1.ytplugin.rest.TimeTrackerRestClient
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class TimeTrackerConnector {


    fun postSavedTimeToServer(repository: YouTrackServer, myProject: Project, savedItems: ConcurrentHashMap<String, Long>) {
        logger.debug("Try posting work item to ${repository.url}")

        val task = object : Task.Modal(myProject, "Post time to YouTrack", true) {
            override fun run(indicator: ProgressIndicator) {
                indicator.text = "Post time tracking items to " + repository.url + "..."
                indicator.fraction = 0.0
                indicator.isIndeterminate = true

                val store = ComponentAware.of(project).spentTimePerTaskStorage
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
            }
        }
        ProgressManager.getInstance().run(task)
    }
}

