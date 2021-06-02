package com.github.jk1.ytplugin.rest

import com.github.jk1.ytplugin.issues.model.Workflow
import com.github.jk1.ytplugin.issues.model.WorkflowRule
import com.github.jk1.ytplugin.logger
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.github.jk1.ytplugin.timeTracker.TrackerNotification
import com.google.gson.JsonObject
import com.intellij.notification.NotificationType
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.utils.URIBuilder

class ScriptsRestClient(override val repository: YouTrackServer) : RestClientTrait, ResponseLoggerTrait {

    fun getScriptsWithRules(): List<Workflow> {

        val builder = URIBuilder("${repository.url}/api/admin/workflows")
        builder.setParameter("\$top", "-1")
            .setParameter("fields", "name,id,rules(id,name)")
            .setParameter("query", "language:JS,system:null")
        val method = HttpGet(builder.build())

        val scriptsList = mutableListOf<Workflow>()
        return try {
            method.execute { element ->

                val jsonArray = element.asJsonArray
                for (json in jsonArray) {
                    scriptsList.add(Workflow(json as JsonObject))
                }
                scriptsList
            }
        } catch (e: RuntimeException) {
            logger.debug("Unable to load YouTrack Scripts: ${e.message}")
            val trackerNote = TrackerNotification()
            trackerNote.notify("Connection to the YouTrack might be lost", NotificationType.WARNING)
            emptyList()
        }

    }


    fun getScriptsContent(workflow: Workflow, rule: WorkflowRule) {

        val builder = URIBuilder("${repository.url}/api/admin/workflows/${workflow.id}/rules/${rule.id}")
        builder.setParameter("\$top", "-1")
            .setParameter("fields", "script")
        val method = HttpGet(builder.build())

        return method.execute { element ->
            rule.content = element.asJsonObject.get("script").asString
        }
//        logger.warn("Unable to get workFlow content $status: ${method.responseBodyAsLoggedString()}")

    }
}
