package com.github.jk1.ytplugin.rest

import com.github.jk1.ytplugin.logger
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.apache.commons.httpclient.NameValuePair
import org.apache.commons.httpclient.methods.GetMethod
import org.apache.commons.httpclient.methods.PostMethod
import org.apache.commons.httpclient.methods.StringRequestEntity
import java.net.URL
import java.nio.charset.StandardCharsets

class AdminRestClient(override val repository: YouTrackServer) : AdminRestClientBase, RestClientTrait, ResponseLoggerTrait {

    override fun getVisibilityGroups(issueId: String): List<String> {
        val getGroupsUrl = "${repository.url}/api/visibilityGroups"
        val method = PostMethod(getGroupsUrl)
        method.params.contentCharset = "UTF-8"

        val top = NameValuePair("\$top", "-1")
        val fields = NameValuePair("fields", "groupsWithoutRecommended(name),recommendedGroups(name)")
        method.setQueryString(arrayOf(top, fields))

        val res: URL? = this::class.java.classLoader.getResource("admin_body.json")
        val jsonBody = res?.readText()?.replace("{issueId}", issueId, true)

        method.requestEntity = StringRequestEntity(jsonBody, "application/json", StandardCharsets.UTF_8.name())
        return method.connect {
            when (httpClient.executeMethod(method)) {
                200 -> {
                    logger.debug("Successfully fetched visibility groups")
                    listOf("All Users") +
                            parseGroupNames(method, "recommendedGroups") +
                            parseGroupNames(method, "groupsWithoutRecommended")
                }
                else -> {
                    logger.warn("failed to fetch visibility groups: ${method.responseBodyAsLoggedString()}")
                    throw RuntimeException()
                }
            }
        }
    }

    private fun parseGroupNames(method: PostMethod, elem: String): List<String> {
        val myObject: JsonObject = JsonParser.parseReader(method.responseBodyAsReader) as JsonObject
        val recommendedGroups: JsonArray = myObject.get(elem) as JsonArray
        return recommendedGroups.map { it.asJsonObject.get("name").asString }
    }

    override fun getAccessibleProjects(): List<String> {
        val method = GetMethod("${repository.url}/api/admin/projects")
        val fields = NameValuePair("fields", "shortName")
        method.setQueryString(arrayOf(fields))

        return method.connect {
            if (httpClient.executeMethod(method) == 200) {
                logger.debug("Successfully fetched accessible projects")
                val json: JsonArray = JsonParser.parseString(method.responseBodyAsString) as JsonArray
                json.map { it.asJsonObject.get("shortName").asString }
            } else {
                logger.warn("Failed to fetch accessible projects: ${method.responseBodyAsLoggedString()}")
                throw RuntimeException("Failed to fetch accessible projects")
            }
        }
    }
}