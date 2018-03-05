package com.github.jk1.ytplugin.notifications

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.logger
import com.github.jk1.ytplugin.rest.NotificationsRestClient
import com.github.jk1.ytplugin.ui.YouTrackPluginIcons
import com.intellij.concurrency.JobScheduler
import com.intellij.notification.NotificationDisplayType.*
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.AbstractProjectComponent
import com.intellij.openapi.project.Project
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import javax.swing.SwingUtilities

class NotificationsComponent(override val project: Project) :
        AbstractProjectComponent(project), ComponentAware, IdeNotificationsTrait {

    private val group = NotificationGroup("YouTrack Notifications", STICKY_BALLOON, true, null, YouTrackPluginIcons.YOUTRACK)
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
                    diff.forEach { handleNotification(it) }
                } catch (e: UnsupportedOperationException) {
                    logger.info(e.message)
                    timedRefreshTask.cancel(false)
                } catch (e: Exception) {
                    logger.warn(e)
                }
            }
        }
    }

    private fun handleNotification(incoming: YouTrackNotification) {
        SwingUtilities.invokeLater {
            with(incoming) {
                val notification = group.createNotification(id, summary, content, NotificationType.INFORMATION, null)
                notification.addAction(BrowseNotificationAction(incoming))
                notification.addAction(DismissNotificationAction(notification))
                notification.addAction(ConfigureNotificationsAction(incoming))
                notification.notify(null)
            }
        }
    }
}