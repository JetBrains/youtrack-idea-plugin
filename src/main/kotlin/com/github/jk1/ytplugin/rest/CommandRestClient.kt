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
        val res: URL? = this::class.java.classLoader.getResource("get_command_body.json")
        val id = command.issue.id
        val jsonBody = res?.readText()
                ?.replace("{query}", command.command, true)
                ?.replace("0", caret.toString(), true)
                ?.replace("{id}", id, true)

        method.requestEntity = StringRequestEntity(jsonBody, "application/json", "UTF-8")

        return method.connect {
            val status = httpClient.executeMethod(method)
            if (status == 200) {
                logger.debug("Successfully posted assist command in CommandRestClient: code $status")
                CommandAssistResponse(method.responseBodyAsLoggedStream())
            } else {
                logger.debug("Runtime Exception while posting assist command in CommandRestClient. " +
                        "\"HTTP $status: ${method.responseBodyAsLoggedString()}")
                throw RuntimeException("HTTP $status: ${method.responseBodyAsLoggedString()}")
            }
        }
    }

    private fun getGroupId(command: YouTrackCommandExecution): String {
        val execUrl = "${repository.url}/api/groups?fields=name,id,allUsersGroup"
        val getMethod = GetMethod(execUrl)
        try {
            val status = httpClient.executeMethod(getMethod)
            val response: JsonArray = JsonParser.parseString(getMethod.responseBodyAsLoggedString()) as JsonArray
            return if (status == 200) {
                logger.debug("Successfully fetched Group Id in CommandRestClient: code $status")
                if (command.commentVisibleGroup == "All Users") {
                    response.first { it.asJsonObject.get("allUsersGroup").asBoolean }
                } else {
                    response.first { command.commentVisibleGroup == it.asJsonObject.get("name").asString }
                }.asJsonObject.get("id").asString
            } else {
                logger.warn("Failed to fetch Group Id in CommandRestClient ")
                ""
            }
        } catch (e: ClassCastException){
            logger.debug("Failed to fetch Group Id in CommandRestClient: attempt to cast JsonObject to JsonArray: " +
                    " ${e.message}")
            logger.debug("Note: access might be forbidden")
        }
        return ""
    }

    override fun executeCommand(command: YouTrackCommandExecution): CommandExecutionResponse {
        val execPostUrl = "${repository.url}/api/commands"
        val postMethod = PostMethod(execPostUrl)
        val comment = command.comment ?: ""
        val res = this::class.java.classLoader.getResource("command_execution_rest.json")
                ?: throw IllegalStateException("Resource 'command_execution_rest.json' file is missing")
        var jsonBody = res.readText()
        if (command.commentVisibleGroup == "All Users") {
            jsonBody += "\n}"
        } else {
            val groupId: String = getGroupId(command)
            if (groupId != ""){
                jsonBody += COMMANDS_GROUP_VISIBILITY
                jsonBody = jsonBody.replace("{groupId}", groupId, true)
            } else {
                jsonBody += "\n}"
            }
        }

        jsonBody = jsonBody.replace("{idReadable}", command.issue.id, true)
                .replace("true", command.silent.toString(), true)
                .replace("{comment}", comment, true)
                .replace("{query}", command.command, true)

        postMethod.requestEntity = StringRequestEntity(jsonBody, "application/json", "UTF-8")
        return postMethod.connect {
            try {
                val status = httpClient.executeMethod(postMethod)
                if (status != 200) {
                    logger.debug("Failed to fetch execute command in CommandRestClient: " +
                            "code $status ${postMethod.responseBodyAsLoggedString()}")
                    val error = JsonParser.parseReader(postMethod.responseBodyAsReader).asJsonObject.get("value").asString
                    CommandExecutionResponse(errors = listOf("Workflow: $error"))
                } else {
                    logger.debug("Successfully fetched execute command in CommandRestClient: code $status")
                    postMethod.responseBodyAsLoggedString()
                    CommandExecutionResponse()
                }
            } catch (e: IllegalStateException){
                logger.debug("Failed to fetch execute command in CommandRestClient: ${e.message}" )
                val error = JsonParser.parseReader(postMethod.responseBodyAsReader).asJsonObject.get("error_description").asString
                CommandExecutionResponse(errors = listOf("Workflow: $error"))
            }
        }
    }
}