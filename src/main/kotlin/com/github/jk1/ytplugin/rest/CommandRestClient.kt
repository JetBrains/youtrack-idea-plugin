package com.github.jk1.ytplugin.rest

import com.github.jk1.ytplugin.commands.model.CommandAssistResponse
import com.github.jk1.ytplugin.commands.model.CommandExecutionResponse
import com.github.jk1.ytplugin.commands.model.YouTrackCommand
import com.github.jk1.ytplugin.commands.model.YouTrackCommandExecution
import com.github.jk1.ytplugin.logger
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.google.gson.JsonParser
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.utils.URIBuilder

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
        val builder = URIBuilder("${repository.url}/api/commands/assist")
        builder.addParameter("fields", "commands(description,error),styleRanges(length,start,style)," +
                "suggestions(caret,completionEnd,completionStart,description,matchingEnd,matchingStart,option,prefix,suffix)")
        val method = HttpPost(builder.build())
        val caret = command.caret - 1
        val res = this::class.java.classLoader.getResource("get_command_body.json")
                ?: throw IllegalStateException("Resource 'get_command_body.json' file is missing")
        val id = command.issue.entityId
        method.entity = res.readText()
                .replace("{query}", command.command, true)
                .replace("0", caret.toString(), true)
                .replace("{id}", id, true)
                .jsonEntity
        return method.execute {
            CommandAssistResponse(it)
        }
    }

    private fun getGroupId(command: YouTrackCommandExecution): String {
        val builder = URIBuilder("${repository.url}/api/groups")
        builder.setParameter("fields", "name,id,allUsersGroup")
        val getMethod = HttpGet(builder.build())
        return try {
            getMethod.execute { element ->
                visibilityState = VisibilityState.VISIBILITY_RESTRICTED
                element.asJsonArray.first {
                    command.commentVisibleGroup == it.asJsonObject.get("name").asString
                }.asJsonObject.get("id").asString
            }
        } catch (e: Exception) {
            // todo: detailed exception handling
            logger.warn("Failed to fetch possible groups ids in CommandRestClient", e)
            ""
        }
    }

    private fun constructJsonForCommandExecution(command: YouTrackCommandExecution): String {
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
        val postMethod = HttpPost("${repository.url}/api/commands")
        postMethod.entity = constructJsonForCommandExecution(command).jsonEntity
        return try {
            postMethod.execute {
                CommandExecutionResponse()
            }
        } catch (e: Exception) {
            // todo: expose more error response details and reimplement it
            try {
                val error = JsonParser.parseString(e.message).asJsonObject.get("error_description")
                val message = error?.asString ?: "Command execution error. See IDE log for details."
                CommandExecutionResponse(errors = listOf(message))
            } catch (ex: Exception) {
                CommandExecutionResponse(errors = listOf("Command execution error. See IDE log for details."))
            }
        }
    }
}

enum class VisibilityState {
    NO_VISIBILITY_RESTRICTIONS,
    VISIBILITY_RESTRICTED
}