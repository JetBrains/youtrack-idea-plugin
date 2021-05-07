package com.github.jk1.ytplugin.rest

import com.github.jk1.ytplugin.issues.model.Workflow
import com.github.jk1.ytplugin.issues.model.WorkflowRule
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.google.gson.JsonObject
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
        return method.execute { element ->
            val jsonArray = element.asJsonArray
            for (json in jsonArray) {
                scriptsList.add(Workflow(json as JsonObject))
            }
            scriptsList
        }
    }


    fun getScriptsContent(workflow: Workflow, rule: WorkflowRule) {

        val builder = URIBuilder("${repository.url}/api/admin/workflows/${workflow.id}/rules/${rule.id}")
        builder.setParameter("\$top", "-1")
                .setParameter("fields", "script")
        val method = HttpGet(builder.build())

        return method.execute { element ->
            rule.content =  element.asJsonObject.get("script").asString
        }
//        logger.warn("Unable to get workFlow content $status: ${method.responseBodyAsLoggedString()}")

    }
}
