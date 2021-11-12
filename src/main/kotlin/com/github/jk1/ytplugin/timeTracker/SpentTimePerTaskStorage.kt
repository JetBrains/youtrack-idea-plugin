package com.github.jk1.ytplugin.timeTracker

import com.intellij.openapi.components.Service
import com.intellij.tasks.LocalTask
import java.util.concurrent.ConcurrentHashMap

@Service
class SpentTimePerTaskStorage {

    private val store = ConcurrentHashMap<LocalTask, Long>()

    fun getSavedTimeForLocalTask(task: LocalTask) : Long? {
        return store[task]
    }
    fun setSavedTimeForLocalTask(task: LocalTask, time: Long) {
        store[task] = store[task]?.plus(time) ?: time
        println ("Time for ${task.id} is ${store[task]}")
    }

}