package com.github.jk1.ytplugin.rest

import com.github.jk1.ytplugin.commands.model.CommandAssistResponse
import com.github.jk1.ytplugin.commands.model.CommandExecutionResponse
import com.github.jk1.ytplugin.commands.model.YouTrackCommand
import com.github.jk1.ytplugin.commands.model.YouTrackCommandExecution
import com.github.jk1.ytplugin.logger
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import org.apache.commons.httpclient.NameValuePair
import org.apache.commons.httpclient.methods.GetMethod
import org.apache.commons.httpclient.methods.PostMethod
import org.apache.commons.httpclient.methods.StringRequestEntity
import java.net.URL

class CommandRestClient(override val repository: YouTrackServer) : CommandRestClientBase, RestClientTrait, ResponseLoggerTrait {

    private var visibilityState = VisibilityState.NO_VISIBILITY_RESTRICTIONS

    companion object {
        const val COMMANDS_GROUP_VISIBILITY = """,
              "visibility": {
                "${"$"}type": "CommandLimitedVisibility",
                "permittedGroups": [
                  {
                    "id": "{groupId}"
                  }
                ]
              }
            }"""
    }

    override fun assistCommand(command: YouTrackCommand): CommandAssistResponse {
        val method = PostMethod("${repository.url}/api/commands/assist")
        val fields = NameValuePair("fields", "commands(description,error),styleRanges(length,start,style)," +
                "suggestions(caret,completionEnd,completionStart,description,matchingEnd,matchingStart,option,prefix,suffix)")
        method.setQueryString(arrayOf(fields))
        val caret = command.caret - 1
        val res = this::class.java.classLoader.getResource("get_command_body.json")
                ?: throw IllegalStateException("Resource 'get_command_body.json' file is missing")

        val id = command.issue.entityId
        val jsonBody = res.readText()
                .replace("{query}", command.command, true)
                .replace("0", caret.toString(), true)
                .replace("{id}", id, true)

        method.requestEntity = StringRequestEntity(jsonBody, "application/json", "UTF-8")

        return method.connect2 {
            val status = httpClient.executeMethod(method)
            if (status == 200) {
                logger.debug("Successfully posted assist command in CommandRestClient: code $status")
                CommandAssistResponse(method.responseBodyAsLoggedStream())
            } else {
                method.responseBodyAsLoggedString()
                logger.error("Runtime Exception while posting assist command in CommandRestClient.")
                throw RuntimeException("HTTP $status")
            }
        }
    }

    private fun getGroupId(command: YouTrackCommandExecution): String {
        val execUrl = "${repository.url}/api/groups?fields=name,id,allUsersGroup"
        val getMethod = GetMethod(execUrl)
        val status = httpClient.executeMethod(getMethod)
        return if (JsonParser.parseString(getMethod.responseBodyAsLoggedString()).isJsonArray ){
            val response: JsonArray = JsonParser.parseString(getMethod.responseBodyAsLoggedString()) as JsonArray
            if (status == 200) {
                logger.debug("Successfully fetched Group Id in CommandRestClient: code $status")
                visibilityState = VisibilityState.VISIBILITY_RESTRICTED
                response.first { command.commentVisibleGroup == it.asJsonObject.get("name").asString }.asJsonObject.get("id").asString
            } else {
                logger.warn("Failed to fetch Group Id in CommandRestClient")
                ""
            }
        } else {
            logger.warn("Failed to fetch possible groups ids in CommandRestClient ")
            ""
        }

    }

    private fun constructJsonForCommandExecution(command: YouTrackCommandExecution) : String {
        val res = this::class.java.classLoader.getResource("command_execution_rest.json")
                ?: throw IllegalStateException("Resource 'command_execution_rest.json' file is missing")

        var jsonBody = res.readText()
        if (command.commentVisibleGroup == "All Users") {
            jsonBody += "\n}"
        } else {
            val groupId: String = getGroupId(command)
            if (visibilityState == VisibilityState.VISIBILITY_RESTRICTED) {
                jsonBody += COMMANDS_GROUP_VISIBILITY
                jsonBody = jsonBody.replace("{groupId}", groupId, true)
            } else {
                jsonBody += "\n}"
            }
        }
        val comment = command.comment ?: ""

        jsonBody = jsonBody.replace("{idReadable}", command.issue.id, true)
                .replace("true", command.silent.toString(), true)
                .replace("{comment}", comment, true)
                .replace("{query}", command.command, true)

        return jsonBody
    }

    override fun executeCommand(command: YouTrackCommandExecution): CommandExecutionResponse {

        val execPostUrl = "${repository.url}/api/commands"
        val postMethod = PostMethod(execPostUrl)
        val jsonBody = constructJsonForCommandExecution(command)

        postMethod.requestEntity = StringRequestEntity(jsonBody, "application/json", "UTF-8")
        return postMethod.connect {
            val status = httpClient.executeMethod(postMethod)
            if (status != 200) {
                val error = JsonParser.parseReader(postMethod.responseBodyAsReader).asJsonObject.get("value").asString
                CommandExecutionResponse(errors = listOf("Workflow: $error"))
            } else {
                postMethod.responseBodyAsLoggedString()
                CommandExecutionResponse()
            }
        }
    }
}

enum class VisibilityState {
    NO_VISIBILITY_RESTRICTIONS,
    VISIBILITY_RESTRICTED
}