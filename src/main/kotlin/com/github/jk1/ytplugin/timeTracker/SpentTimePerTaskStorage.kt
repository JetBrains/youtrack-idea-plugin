package com.github.jk1.ytplugin.timeTracker

import com.github.jk1.ytplugin.logger
import com.intellij.openapi.components.Service
import com.intellij.tasks.LocalTask
import java.util.concurrent.ConcurrentHashMap

@Service
class SpentTimePerTaskStorage {

    private val store = ConcurrentHashMap<LocalTask, Long>()

    fun getSavedTimeForLocalTask(task: LocalTask) : Long {
        logger.debug("Stored time for ${task.id} is obtained: ${store[task]?.let { TimeTracker.formatTimePeriod(it) }}")
        return store[task] ?: 0
    }

    fun setSavedTimeForLocalTask(task: LocalTask, time: Long) {
        store[task] = store[task]?.plus(time) ?: time
        logger.debug("Time for ${task.id} is saved: ${store[task]?.let { TimeTracker.formatTimePeriod(it) }}")
    }

    fun resetSavedTimeForLocalTask(task: LocalTask) {
        store[task] = 0
        logger.debug("Time for ${task.id} is reset")
    }
}