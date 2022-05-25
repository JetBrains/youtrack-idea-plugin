package com.github.jk1.ytplugin.rest

import com.github.jk1.ytplugin.logger
import com.github.jk1.ytplugin.rest.MulticatchException.Companion.multicatchException
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.github.jk1.ytplugin.timeTracker.TrackerNotification
import com.google.gson.Gson
import com.intellij.notification.NotificationType
import org.apache.http.HttpResponse
import org.apache.http.HttpStatus
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.utils.URIBuilder
import org.apache.http.conn.HttpHostConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException


class TimeTrackerRestClient(override val repository: YouTrackServer) : RestClientTrait, ResponseLoggerTrait {

    fun postNewWorkItem(
        issueId: String, time: String, type: String, comment: String,
        date: String, attributes: Map<String, String> = mapOf()
    ): Int {
        val types = getAvailableWorkItemTypes()

        val method = HttpPost("${repository.url}/api/issues/${issueId}/timeTracking/workItems")
        val res: URL? = this::class.java.classLoader.getResource("post_work_item_body.json")
        val g = Gson()

        val jsonBody = res?.readText()
            ?.replace("\"{minutes}\"", time, true)
            ?.replace("\"{date}\"", date, true)
            ?.replace("{authorId}", getMyIdAsAuthor(), true)
            ?.replace("{type}", g.toJson(type).trim('\"'), true)
            ?.replace(
                "{typeId}",
                types[type] ?: throw IllegalArgumentException("No work item type by name '$type'"),
                true
            )
            ?.replace("{comment}", g.toJson(comment).trim('\"'), true)
            ?.replace("\"{attributes}\"", constructAttributesJson(attributes))
        method.entity = jsonBody?.jsonEntity

        return try {
            val response: HttpResponse = httpClient.execute(method)
            response.statusLine.statusCode
        } catch (e: RuntimeException) {
            logger.debug(e)
            HttpStatus.SC_BAD_REQUEST
        } catch (e: Exception) {
            logger.debug(e)
            HttpStatus.SC_BAD_REQUEST
        }
    }

    private fun constructAttributesJson(attributes: Map<String, String>): String {
        var attributesString = ""
        attributes.forEach {
            val res: URL? = this::class.java.classLoader.getResource("work_item_attribute_template.json")
            attributesString += "\n${
                (res?.readText()
                    ?.replace("{attributeName}", it.key, true)
                    ?.replace("{attributeValueName}", it.value, true))
            },"
        }
        return attributesString.removeSuffix(",")
    }

    private fun getMyIdAsAuthor(): String {
        return try {
            HttpGet("${repository.url}/api/admin/users/me")
                .execute {
                    it.asJsonObject.get("id").asString
                }
        } catch (e: Exception) {
            logger.debug(e)
            ""
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
            e.multicatchException(
                SocketException::class.java,
                UnknownHostException::class.java,
                SocketTimeoutException::class.java,
                HttpHostConnectException::class.java
            ) {
                val trackerNote = TrackerNotification()
                trackerNote.notify(
                    "Connection to YouTrack server is lost, please check your network connection",
                    NotificationType.WARNING
                )
                logger.warn("Connection to network lost: ${e.message}")
                mapOf()
            }
        }
    }
}