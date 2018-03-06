package com.github.jk1.ytplugin.rest

import com.github.jk1.ytplugin.notifications.YouTrackNotification
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.google.gson.JsonParser
import org.apache.commons.httpclient.methods.GetMethod
import java.io.InputStreamReader

class NotificationsRestClient(override val repository: YouTrackServer) : RestClientTrait, ResponseLoggerTrait {

    fun getNotifications(): List<YouTrackNotification>{
        val method = GetMethod("${repository.url}/api/users/notifications?fields=id,content,metadata")
        method.setRequestHeader("Accept", "application/json")
        return method.connect {
            val status = httpClient.executeMethod(it)
            if (status == 200) {
                val stream = InputStreamReader(it.responseBodyAsLoggedStream(), "UTF-8")
                JsonParser().parse(stream).asJsonArray.map { YouTrackNotification(it, repository.url) }
            } else if (status == 404) {
                // persistent notifications are supported starting from YouTrack 2018.1
                throw UnsupportedOperationException("Current YouTrack version doesn't support notification persistence")
            } else {
                throw RuntimeException(method.responseBodyAsLoggedString())
            }
        }
    }
}