package com.github.jk1.ytplugin.components

import com.intellij.openapi.components.ProjectComponent
import com.intellij.tasks.Task
import com.intellij.tasks.TaskManager
import com.intellij.tasks.impl.BaseRepository
import com.intellij.tasks.youtrack.YouTrackRepository
import org.apache.commons.httpclient.HttpClient


interface TaskManagerProxyComponent : ProjectComponent {

    fun getTaskManager(): TaskManager

    fun getYouTrackRepository(): YouTrackRepository

    fun getActiveTask(): Task

    fun getRestClient() : HttpClient
}