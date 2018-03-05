package com.github.jk1.ytplugin.notifications

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.logger
import com.github.jk1.ytplugin.rest.NotificationsRestClient
import com.intellij.concurrency.JobScheduler
import com.intellij.openapi.components.AbstractProjectComponent
import com.intellij.openapi.project.Project
import com.sun.glass.ui.Application
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class NotificationsComponent(override val project: Project) :
        AbstractProjectComponent(project), ComponentAware, IdeNotificationsTrait {

    private val notifications: MutableMap<String, List<YouTrackNotification>> = ConcurrentHashMap()

    private lateinit var timedRefreshTask: ScheduledFuture<*>

    override fun initComponent() {
        //  todo: customizable update interval
        timedRefreshTask = JobScheduler.getScheduler().scheduleWithFixedDelay({ update() }, 0, 5, TimeUnit.MINUTES)
    }

    override fun disposeComponent() {
        timedRefreshTask.cancel(true)
    }

    private fun update() {
        if (!project.isDisposed) {
            taskManagerComponent.getAllConfiguredYouTrackRepositories().forEach {
                try {
                    val remote = NotificationsRestClient(it).getNotifications()
                    val local = notifications[it.id] ?: listOf()
                    val diff = remote.drop(local.size)
                    notifications[it.id] = remote
                    diff.forEach { notification ->
                        Application.invokeLater { showNotification("Notification", notification.text) }
                    }
                } catch (e: Exception) {
                    logger.warn(e)
                }
            }
        }
    }
}