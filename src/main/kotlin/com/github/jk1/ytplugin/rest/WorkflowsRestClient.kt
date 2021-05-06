package com.github.jk1.ytplugin.rest

import com.github.jk1.ytplugin.issues.model.Workflow
import com.github.jk1.ytplugin.issues.model.WorkflowRule
import com.github.jk1.ytplugin.logger
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.google.gson.JsonObject
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.utils.URIBuilder

class WorkflowsRestClient(override val repository: YouTrackServer) : RestClientTrait, ResponseLoggerTrait {

    fun getWorkflowWithRules(workflowName: String): Workflow? {

        val builder = URIBuilder("${repository.url}/api/admin/workflows")
        builder.setParameter("\$top", "-1")
                .setParameter("fields", "name,id,rules(id,name)")
                .setParameter("query", "language:JS,system:null")
        val method = HttpGet(builder.build())

        return method.execute { element ->
            var workflow: Workflow? = null
            val jsonArray = element.asJsonArray
            for (json in jsonArray) {
                if (json.asJsonObject.get("name").asString.contains(workflowName)) {
                    workflow = Workflow(json as JsonObject)
                    break
                }
            }
            logger.warn("Workflow not found: ${element.asString}")
            workflow
        }
//     logger.warn("failed to fetch workflow rules: ${method.responseBodyAsLoggedString()}")
    }


    fun getWorkFlowContent(workflow: Workflow, rule: WorkflowRule) {

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
