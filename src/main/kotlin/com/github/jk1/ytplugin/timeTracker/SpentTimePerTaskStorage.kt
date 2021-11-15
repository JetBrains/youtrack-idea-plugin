package com.github.jk1.ytplugin.timeTracker

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.logger
import com.intellij.ide.util.PropertiesComponent
import com.intellij.ide.util.PropertyName
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.tasks.LocalTask
import java.util.concurrent.ConcurrentHashMap

@Service
class SpentTimePerTaskStorage(override val project: Project) : ComponentAware {

    @PropertyName("spentTimePerTaskStorage.store")
    private val store = ConcurrentHashMap<LocalTask, Long>()

    init {
        val propertiesStore: PropertiesComponent = PropertiesComponent.getInstance(project)
        propertiesStore.loadFields(this)
    }

    fun getSavedTimeForLocalTask(task: LocalTask) : Long {
        logger.debug("Stored time for ${task.id} is obtained: ${store[task]?.let { TimeTracker.formatTimePeriod(it) }}")
        return store[task] ?: 0
    }

    fun setSavedTimeForLocalTask(task: LocalTask, time: Long) {
        if (time > 0){
            store[task] = store[task]?.plus(time) ?: time

            val propertiesStore: PropertiesComponent = PropertiesComponent.getInstance(project)
            propertiesStore.saveFields(this)

            logger.debug("Time for ${task.id} is saved: ${store[task]?.let { TimeTracker.formatTimePeriod(it) }}")
        } else {
            logger.debug("Recorded time for ${task.id} = 0. No need to save.")
        }
    }

    fun resetSavedTimeForLocalTask(task: LocalTask) {
        store[task] = 0
        logger.debug("Time for ${task.id} is reset")
    }


    fun getAllStoredItems() : ConcurrentHashMap<LocalTask, Long> {
        logger.debug("Stored time for all issues obtained")
        return store
    }
}