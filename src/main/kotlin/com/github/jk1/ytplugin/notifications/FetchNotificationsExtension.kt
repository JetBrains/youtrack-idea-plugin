package com.github.jk1.ytplugin.notifications

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.logger
import com.github.jk1.ytplugin.rest.NotificationsRestClient
import com.github.jk1.ytplugin.ui.YouTrackPluginIcons
import com.intellij.concurrency.JobScheduler
import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.NotificationDisplayType.STICKY_BALLOON
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.util.Disposer
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import javax.swing.SwingUtilities

class FetchNotificationsExtension : StartupActivity.Background {

    companion object {
        private const val PERSISTENT_KEY = "com.jetbrains.youtrack.notifications"
        private val group = NotificationGroup("YouTrack Notifications", STICKY_BALLOON, true, null, YouTrackPluginIcons.YOUTRACK)
    }

    override fun runActivity(project: Project) {
        //  todo: customizable update interval
        val timedRefreshTask: ScheduledFuture<*> =
                JobScheduler.getScheduler().scheduleWithFixedDelay({ update(project) }, 15, 60, TimeUnit.SECONDS)
        Disposer.register(ComponentAware.of(project).sourceNavigatorComponent, // any project-level disposable will do
                Disposable { timedRefreshTask.cancel(false) })
    }

    private fun update(project: Project) {
        if (!project.isDisposed) {
            ComponentAware.of(project).taskManagerComponent.getAllConfiguredYouTrackRepositories().forEach {
                logger.debug("Fetching notifications from YouTrack server ${it.url}")
                try {
                    val notifications = NotificationsRestClient(it).getNotifications()
                    val unseen = notifications.filterUnseen()
                    logger.debug("Fetched ${notifications.size} notifications, ${unseen.size} new")
                    unseen.forEach { notification -> handleNotification(notification, project) }
                    saveAsSeen(notifications, project)
                }  catch (e: Exception) {
                    logger.warn("Failed to fetch notifications from YouTrack server: ${e.message}" )
                    logger.debug(e)
                }
            }
        }
    }

    private fun handleNotification(incoming: YouTrackNotification, project: Project) {
        if (incoming.recipient == "guest" ) {
            logger.warn("Notification ${incoming.id} is intended for the guest user, dropping")
        } else {
            SwingUtilities.invokeLater {
                with(incoming) {
                    val notification = group.createNotification(issueId, summary, content, NotificationType.INFORMATION, null)
                    notification.addAction(BrowseNotificationAction(incoming))
                    notification.addAction(DismissNotificationAction(notification))
                    notification.addAction(ConfigureNotificationsAction(incoming))
                    notification.notify(project)
                }
            }
        }
    }

    private fun saveAsSeen(notifications: List<YouTrackNotification>, project: Project) {
        PropertiesComponent.getInstance(project).setValue(PERSISTENT_KEY, notifications.joinToString(" ") { it.id })
    }

    private fun List<YouTrackNotification>.filterUnseen(): List<YouTrackNotification> {
        val ids = PropertiesComponent.getInstance().getValue(PERSISTENT_KEY, "").split(" ").toSet()
        return this.filter { !ids.contains(it.id) }
    }
}