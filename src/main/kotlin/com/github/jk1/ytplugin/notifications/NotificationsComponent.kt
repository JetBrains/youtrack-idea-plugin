package com.github.jk1.ytplugin.notifications

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.logger
import com.github.jk1.ytplugin.rest.NotificationsRestClient
import com.github.jk1.ytplugin.ui.YouTrackPluginIcons
import com.intellij.concurrency.JobScheduler
import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.NotificationDisplayType.*
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.AbstractProjectComponent
import com.intellij.openapi.project.Project
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import javax.swing.SwingUtilities

class NotificationsComponent(override val project: Project) : AbstractProjectComponent(project), ComponentAware {

    private val PERSISTENT_KEY = "com.jetbrains.youtrack.notifications"
    private val group = NotificationGroup("YouTrack Notifications", STICKY_BALLOON, true, null, YouTrackPluginIcons.YOUTRACK)
    private lateinit var timedRefreshTask: ScheduledFuture<*>

    override fun initComponent() {
        //  todo: customizable update interval
        timedRefreshTask = JobScheduler.getScheduler().scheduleWithFixedDelay({ update() }, 15, 300, TimeUnit.SECONDS)
    }

    override fun disposeComponent() {
        timedRefreshTask.cancel(true)
    }

    fun update() {
        if (!project.isDisposed) {
            taskManagerComponent.getAllConfiguredYouTrackRepositories().forEach {
                logger.debug("Fetching notifications from YouTrack server ${it.url}")
                try {
                    val notifications = NotificationsRestClient(it).getNotifications()
                    val unseen = notifications.filterUnseen()
                    logger.debug("Fetched ${notifications.size} notifications, ${unseen.size} new")
                    unseen.forEach { handleNotification(it) }
                    saveAsSeen(notifications)
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
                val notification = group.createNotification(issueId, summary, content, NotificationType.INFORMATION, null)
                notification.addAction(BrowseNotificationAction(incoming))
                notification.addAction(DismissNotificationAction(notification))
                notification.addAction(ConfigureNotificationsAction(incoming))
                notification.notify(null)
            }
        }
    }

    private fun saveAsSeen(notifications: List<YouTrackNotification>) {
        PropertiesComponent.getInstance().setValue(PERSISTENT_KEY, notifications.joinToString(" ") { it.id })
    }

    private fun List<YouTrackNotification>.filterUnseen(): List<YouTrackNotification> {
        val ids = PropertiesComponent.getInstance().getValue(PERSISTENT_KEY, "").split(" ").toSet()
        return this.filter { !ids.contains(it.id) }
    }
}