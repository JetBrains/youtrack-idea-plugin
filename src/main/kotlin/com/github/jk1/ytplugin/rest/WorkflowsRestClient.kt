package com.github.jk1.ytplugin.rest

import com.github.jk1.ytplugin.issues.model.Workflow
import com.github.jk1.ytplugin.issues.model.WorkflowRule
import com.github.jk1.ytplugin.logger
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.apache.commons.httpclient.NameValuePair
import org.apache.commons.httpclient.methods.GetMethod

class WorkflowsRestClient(override val repository: YouTrackServer) : RestClientTrait, ResponseLoggerTrait {

    fun getWorkflowRulesList(workflowName: String): Workflow? {
        val getWorkflowsListUrl = "${repository.url}/api/admin/workflows"
        val method = GetMethod(getWorkflowsListUrl)
        method.params.contentCharset = "UTF-8"

        val top = NameValuePair("\$top", "-1")
        val fields = NameValuePair("fields", "name,id,rules(id,name)")
        val query = NameValuePair("query", "language:JS,system:null")
        method.setQueryString(arrayOf(top, fields, query))

        return method.connect {
            var workflow: Workflow? = null
            when (val status = httpClient.executeMethod(method)) {
                200 -> {
                    logger.debug("Successfully fetched workflow rules list: code $status")
                    val jsonArray: JsonArray = JsonParser.parseString(method.responseBodyAsString) as JsonArray
                    for (json in jsonArray){
                        if (json.asJsonObject.get("name").asString.contains(workflowName)){
                            workflow = Workflow(json as JsonObject)
                            break
                        }
                    }
                    logger.warn("Workflow not found: ${method.responseBodyAsLoggedString()}")
                    workflow
                }
                else -> {
                    logger.warn("failed to fetch workflow rules: ${method.responseBodyAsLoggedString()}")
                    throw RuntimeException()
                }
            }
        }
    }


    fun getWorkFlowContent(workflow: Workflow, rule: WorkflowRule) {
        val url = "${repository.url}/api/admin/workflows/${workflow.id}/rules/${rule.id}"
        val myFields = NameValuePair("fields", "script")
        val myTop = NameValuePair("\$top", "-1")
        val method = GetMethod(url)
        method.setQueryString(arrayOf(myTop, myFields))
        return method.connect {
            when (val status = httpClient.executeMethod(method)) {
                200 -> {
                    rule.content = JsonParser.parseString(method.responseBodyAsString).asJsonObject.get("script").asString
                }
                else -> {
                    logger.warn("Unable to get workFlow content $status: ${method.responseBodyAsLoggedString()}")
                }
            }
        }
    }
}
