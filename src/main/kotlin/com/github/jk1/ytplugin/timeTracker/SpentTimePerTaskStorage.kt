package com.github.jk1.ytplugin.timeTracker

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.logger
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.ide.util.PropertiesComponent
import com.intellij.ide.util.PropertyName
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.tasks.LocalTask
import java.util.concurrent.ConcurrentHashMap

@Service
class SpentTimePerTaskStorage(override val project: Project) : ComponentAware {

    private var store = ConcurrentHashMap<String, Long>()

    @PropertyName("spentTimePerTaskStorage.store")
    private var storeJson = ""

    init {
        val propertiesStore: PropertiesComponent = PropertiesComponent.getInstance(project)
        propertiesStore.loadFields(this)
        populateStoreFromJson(storeJson)
    }

    fun getSavedTimeForLocalTask(task: LocalTask) : Long {
        logger.debug("Stored time for ${task.id} is obtained: ${store[task.id]?.let { TimeTracker.formatTimePeriod(it) }}")
        return store[task.id] ?: 0
    }

    fun setSavedTimeForLocalTask(task: LocalTask, time: Long) {
        if (time > 60000){ // more than 1 min
            store[task.id] = store[task.id]?.plus(time) ?: time
            storeJson = createStoreJson()

            val propertiesStore: PropertiesComponent = PropertiesComponent.getInstance(project)
            propertiesStore.saveFields(this)

            logger.debug("Time for ${task.id} is saved: ${store[task.id]?.let { TimeTracker.formatTimePeriod(it) }}")
        } else {
            logger.debug("Recorded time for ${task.id} = 0. No need to save.")
        }
    }

    fun resetSavedTimeForLocalTask(task: String) {
        store.remove(task)
        storeJson = ""

        logger.debug("Time for $task is reset")
    }

    fun getAllStoredItems() : ConcurrentHashMap<String, Long> {
        logger.debug("Stored time for all issues obtained")
        return store
    }

    private fun createStoreJson(): String {

        val storeJsonArray = JsonArray()
        store.forEach { entry ->
            val element = JsonObject()
            element.addProperty("id", entry.key)
            element.addProperty("time", entry.value)

            storeJsonArray.add(element)
        }

        return storeJsonArray.toString()

    }

    private fun populateStoreFromJson(jsonString: String) {
        val json = JsonParser.parseString(jsonString).asJsonArray
        json.forEach { entry -> store[entry.asJsonObject.get("id").asString] = entry.asJsonObject.get("time").asLong}
    }
}