package com.github.jk1.ytplugin.rest

import com.github.jk1.ytplugin.logger
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.utils.URIBuilder
import java.net.URL

class AdminRestClient(override val repository: YouTrackServer) : AdminRestClientBase, RestClientTrait, ResponseLoggerTrait {

    override fun getVisibilityGroups(issueId: String): List<String> {
        val builder = URIBuilder("${repository.url}/api/visibilityGroups")
        builder.setParameter("\$top", "-1")
                .setParameter("fields", "groupsWithoutRecommended(name),recommendedGroups(name)")
        val method = HttpPost(builder.build())
        val res: URL? = this::class.java.classLoader.getResource("admin_body.json")
        val jsonBody = res?.readText()?.replace("{issueId}", issueId, true)
        method.entity = jsonBody?.jsonEntity
        return method.execute {
            listOf("All Users") +
                    parseGroupNames(it.asJsonObject, "recommendedGroups") +
                    parseGroupNames(it.asJsonObject, "groupsWithoutRecommended")
        }
    }

    private fun parseGroupNames(myObject: JsonObject, elem: String): List<String> {
        val recommendedGroups: JsonArray = myObject.get(elem) as JsonArray
        return recommendedGroups.map { it.asJsonObject.get("name").asString }
    }

    override fun getAccessibleProjects(): List<String> {
        val builder = URIBuilder("${repository.url}/api/admin/projects")
        builder.setParameter("fields", "shortName")
        val method = HttpGet(builder.build())
        return method.execute { element ->
            element.asJsonArray.map { it.asJsonObject.get("shortName").asString }
        }
    }

    fun checkIfTrackingIsEnabled(projectId: String): Boolean {
        val builder = URIBuilder("${repository.url}/api/admin/projects/$projectId/timeTrackingSettings")
        builder.setParameter("fields", "enabled")
        val method = HttpGet(builder.build())
        return method.execute {
            if (it.asJsonObject.get("enabled").asBoolean) {
                logger.debug("Time Tracking is enabled for project $projectId")
                true
            } else {
                logger.debug("Time Tracking is disabled for project $projectId")
                false
            }
        }
    }
}
