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
            when (httpClient.executeMethod(it)) {
                200 -> {
                    val streamReader = InputStreamReader(method.responseBodyAsLoggedStream(), "UTF-8")
                    JsonParser.parseReader(streamReader).asJsonArray.map { YouTrackNotification(it, repository.url) }
                }
                404 -> {
                    // persistent notifications are supported starting from YouTrack 2018.1
                    listOf()
                }
                else -> throw RuntimeException(method.responseBodyAsLoggedString())
            }
        }
    }
}