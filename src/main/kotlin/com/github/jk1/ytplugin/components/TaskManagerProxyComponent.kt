package com.github.jk1.ytplugin.components

import com.intellij.openapi.components.ProjectComponent
import com.intellij.tasks.Task
import com.intellij.tasks.TaskManager
import com.intellij.tasks.impl.BaseRepository


interface TaskManagerProxyComponent : ProjectComponent {

    fun getTaskManager(): TaskManager

    fun getYouTrackRepository(): BaseRepository

    fun getActiveTask(): Task
}