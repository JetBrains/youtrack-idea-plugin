package com.github.jk1.ytplugin.rest

import com.github.jk1.ytplugin.logger
import com.github.jk1.ytplugin.notifications.YouTrackNotification
import com.github.jk1.ytplugin.tasks.YouTrackServer
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.utils.URIBuilder

class NotificationsRestClient(override val repository: YouTrackServer) : RestClientTrait, ResponseLoggerTrait {

    fun getNotifications(): List<YouTrackNotification> {
        val builder = URIBuilder("${repository.url}/api/users/notifications")
        builder.setParameter("\$top", "-1")
                .setParameter("fields", "id,recipient(login),content,metadata")
        val method = HttpGet(builder.build())
        return try {
            method.execute { element ->
                element.asJsonArray
                        .map { YouTrackNotification(it, repository.url) }
                        .also { list ->
                            if (list.isEmpty()) {
                                logger.debug("No notifications for current user are available on YouTrack server")
                            } else {
                                logger.debug("Successfully fetched ${list.size} notifications")
                            }
                        }
            }
        } catch (e: Exception) {
            // todo: distinguish 404 from other errors here
            // persistent notifications are supported starting from YouTrack 2018.1
            listOf()
        }
    }
}