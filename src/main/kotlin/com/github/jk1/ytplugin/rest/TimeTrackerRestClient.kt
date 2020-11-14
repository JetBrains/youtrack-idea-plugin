package com.github.jk1.ytplugin.rest

import com.github.jk1.ytplugin.logger
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import org.apache.commons.httpclient.NameValuePair
import org.apache.commons.httpclient.methods.GetMethod
import org.apache.commons.httpclient.methods.PostMethod
import org.apache.commons.httpclient.methods.StringRequestEntity
import java.net.URL
import java.nio.charset.StandardCharsets


class TimeTrackerRestClient(override val repository: YouTrackServer) : RestClientTrait, ResponseLoggerTrait {

    fun postNewWorkItem(issueId: String, time: String, type: String, comment: String, date: String) : Int {
        val getGroupsUrl = "${repository.url}/api/issues/${issueId}/timeTracking/workItems"

        val method = PostMethod(getGroupsUrl)
        method.params.contentCharset = "UTF-8"

        val res: URL? = this::class.java.classLoader.getResource("post_work_item_body.json")
        val jsonBody = res?.readText()
                ?.replace("\"{minutes}\"", time, true)
                ?.replace("\"{date}\"", date, true)
                ?.replace("{type}", type, true)
                ?.replace("{typeId}", findWorkItemId(type), true)
                ?.replace("{comment}", comment, true)

        method.requestEntity = StringRequestEntity(jsonBody, "application/json", StandardCharsets.UTF_8.name())

        return method.connect {
            when (val status = httpClient.executeMethod(method)) {
                200 -> {
                    logger.debug("Successfully posted work item ${findWorkItemId(type)} for issue $issueId with code $status")
                    status
                }
                else -> {
                    logger.warn("Work item ${findWorkItemId(type)}, posting failed with code $status: " + method.responseBodyAsLoggedString())
                    0
                }
            }
        }
    }

    private fun findWorkItemId(name: String) : String {
        val types = getAvailableWorkItemTypes()
        for (type in types) {
            if (type.name == name) {
                logger.debug("$name work item id found: ${type.value}")
                return type.value
            }
        }
        logger.debug("No such work item id found: $name")
        return ""
    }

    fun getAvailableWorkItemTypes() : List<NameValuePair> {
        val url = "${repository.url}/api/admin/timeTrackingSettings/workItemTypes"
        val method = GetMethod(url)
        val myFields = NameValuePair("fields","name,id")
        method.setQueryString(arrayOf( myFields))

        val result = mutableListOf<NameValuePair>()

        return method.connect {
            when (val status = httpClient.executeMethod(method)) {
                200 -> {
                    val types: JsonArray = JsonParser.parseString(method.responseBodyAsString) as JsonArray
                    for (type in types){
                        val currentType = type.asJsonObject
                        val pair = NameValuePair(currentType.asJsonObject.get("name").asString, currentType.asJsonObject.get("id").asString)
                        result.add(pair)
                    }
                    logger.debug("Successfully fetched available work items types: code $status")
                    result
                }
                else -> {
                    logger.warn("Unable to fetch available work items types: ${method.responseBodyAsLoggedString()}")
                    throw RuntimeException()
                }
            }
        }
    }
}