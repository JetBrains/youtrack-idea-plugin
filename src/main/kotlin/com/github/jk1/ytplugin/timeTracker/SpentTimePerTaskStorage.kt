package com.github.jk1.ytplugin.timeTracker

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.logger
import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern


@Service
class SpentTimePerTaskStorage(override val project: Project) : ComponentAware {

    @Volatile
    private var store = ConcurrentHashMap<String, Long>()

    init {
        val propertiesStore: PropertiesComponent = PropertiesComponent.getInstance(project)
        store = convertStringToHashMap(propertiesStore.getValue("spentTimePerTaskStorage.store").toString())
    }

    private fun convertStringToHashMap(string: String?): ConcurrentHashMap<String, Long> {
        val loadedMap = ConcurrentHashMap<String, Long>()
        val pattern: Pattern = Pattern.compile("[\\{\\}\\=\\, ]++")
        val split: Array<String> = pattern.split(string)
        var i = 1
        while (i + 2 <= split.size) {
            loadedMap[split[i]] = split[i + 1].toLong()
            i += 2
        }
        return loadedMap
    }

    fun getSavedTimeForLocalTask(task: String) : Long {
        if (store[task] != null)
            logger.debug("Stored time for $task is obtained: ${store[task]?.let { TimeTracker.formatTimePeriod(it) }}")
        return store[task] ?: 0
    }

    @Synchronized
    fun setSavedTimeForLocalTask(task: String, time: Long) {
        if (time >= 60000){ // more than 1 min
            store[task] = store[task]?.plus(time) ?: time

            val propertiesStore: PropertiesComponent = PropertiesComponent.getInstance(project)
            propertiesStore.setValue("spentTimePerTaskStorage.store", store.toString())

            val trackerNote = TrackerNotification()
            trackerNote.notify("Added " +
                    TimeTracker.formatTimePeriod(getSavedTimeForLocalTask(task)) +
                    " of tracked time for $task to local time tracking records", NotificationType.INFORMATION)

            logger.debug("Time for $task is saved: ${store[task]?.let { TimeTracker.formatTimePeriod(it) }}")
        } else {
            logger.debug("Recorded time for $task = 0. No need to save.")
        }
    }

    @Synchronized
    fun resetSavedTimeForLocalTask(task: String) {
        store.remove(task)

        val propertiesStore: PropertiesComponent = PropertiesComponent.getInstance(project)
        propertiesStore.setValue("spentTimePerTaskStorage.store", store.toString())

        logger.debug("Time for $task is reset")
    }

    fun getAllStoredItems() : ConcurrentHashMap<String, Long> {
        logger.debug("Stored time for all issues obtained")
        return store
    }

    fun removeAllSavedItems() {
        store.clear()
        logger.debug("Stored time for all issues is cleared")

        val propertiesStore: PropertiesComponent = PropertiesComponent.getInstance(project)
        propertiesStore.setValue("spentTimePerTaskStorage.store", store.toString())
    }

}