package com.github.jk1.ytplugin.rest

import com.github.jk1.ytplugin.logger
import com.github.jk1.ytplugin.rest.MulticatchException.Companion.multicatchException
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.github.jk1.ytplugin.timeTracker.TrackerNotification
import com.intellij.notification.NotificationType
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.utils.URIBuilder
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException


class TimeTrackerRestClient(override val repository: YouTrackServer) : RestClientTrait, ResponseLoggerTrait {

    fun postNewWorkItem(issueId: String, time: String, type: String, comment: String, date: String) {
        val types = getAvailableWorkItemTypes()

        val method = HttpPost("${repository.url}/api/issues/${issueId}/timeTracking/workItems")
        val res: URL? = this::class.java.classLoader.getResource("post_work_item_body.json")
        val jsonBody = res?.readText()
                ?.replace("\"{minutes}\"", time, true)
                ?.replace("\"{date}\"", date, true)
                ?.replace("{authorId}", getMyIdAsAuthor(), true)
                ?.replace("{type}", type, true)
                ?.replace("{typeId}", types[type] ?: throw IllegalArgumentException("No work item type by name '$type'"), true)
                ?.replace("{comment}", comment, true)
        method.entity = jsonBody?.jsonEntity
        method.execute {
            logger.debug("Successfully posted work item ${types[type]} with time $time for issue $issueId")
        }
    }

    private fun getMyIdAsAuthor(): String {
        return HttpGet("${repository.url}/api/admin/users/me")
                .execute {
                    it.asJsonObject.get("id").asString
                }
    }

    fun getAvailableWorkItemTypes(): Map<String, String> {
        val builder = URIBuilder("${repository.url}/api/admin/timeTrackingSettings/workItemTypes")
        builder.addParameter("fields", "name,id")
        val method = HttpGet(builder.build())
        return try {
            method.execute { element ->
                element.asJsonArray.associate {
                    Pair(it.asJsonObject.get("name").asString, it.asJsonObject.get("id").asString)
                }
            }
        } catch (e: Exception) {
            e.multicatchException(SocketException::class.java, UnknownHostException::class.java, SocketTimeoutException::class.java) {
                val trackerNote = TrackerNotification()
                trackerNote.notify("Connection to YouTrack server is lost, please check your network connection", NotificationType.WARNING)
                logger.warn("Connection to network lost: ${e.message}")
                mapOf()
            }
        }
    }
}