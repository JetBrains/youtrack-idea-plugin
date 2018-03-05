package com.github.jk1.ytplugin.rest

import com.github.jk1.ytplugin.notifications.YouTrackNotification
import com.github.jk1.ytplugin.tasks.YouTrackServer
import org.apache.commons.httpclient.methods.GetMethod

class NotificationsRestClient(override val repository: YouTrackServer) : RestClientTrait, ResponseLoggerTrait {

    fun getNotifications(): List<YouTrackNotification>{
        val method = GetMethod("${repository.url}/api/users/notifications?fields=recipient(name),sender(name),content,metadata,timestamp")
        return method.execute { it.asJsonArray.map { YouTrackNotification(it) } }
    }
}