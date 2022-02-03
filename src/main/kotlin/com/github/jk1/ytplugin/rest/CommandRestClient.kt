package com.github.jk1.ytplugin.rest

import com.github.jk1.ytplugin.commands.model.CommandAssistResponse
import com.github.jk1.ytplugin.commands.model.CommandExecutionResponse
import com.github.jk1.ytplugin.commands.model.YouTrackCommand
import com.github.jk1.ytplugin.commands.model.YouTrackCommandExecution
import com.github.jk1.ytplugin.logger
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.google.gson.*
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.utils.URIBuilder

class CommandRestClient(override val repository: YouTrackServer) : CommandRestClientBase, RestClientTrait, ResponseLoggerTrait {

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
                .replace("\"{caret}\"", caret.toString(), true)
                .replace("{id}", id, true)
                .jsonEntity
        return method.execute {
            CommandAssistResponse(it)
        }
    }

    private fun getGroupId(command: YouTrackCommandExecution): String {
        // todo: load groups with ids to avoid this lookup completely
        val builder = URIBuilder("${repository.url}/api/groups?\$top=1000")
        builder.setParameter("fields", "name,id,allUsersGroup")
        val getMethod = HttpGet(builder.build())
        return try {
            getMethod.execute { element ->
                element.asJsonArray.first {
                    command.commentVisibleGroup == it.asJsonObject.get("name").asString
                }.asJsonObject.get("id").asString
            }
        } catch (e: Exception) {
            logger.warn("Failed to fetch possible groups ids in CommandRestClient", e)
            throw IllegalStateException("User group '${command.commentVisibleGroup}' cannot be found")
        }
    }

    override fun executeCommand(command: YouTrackCommandExecution): CommandExecutionResponse {
        val postMethod = HttpPost("${repository.url}/api/commands")
        val json = JsonObject()
        val issueJson = JsonObject()
        issueJson.addProperty("idReadable", command.issue.id)
        json.add("issues", JsonArray().also { it.add(issueJson) })
        json.addProperty("query", command.command)
        json.addProperty("silent", command.silent)
        json.addProperty("comment", command.comment)
        if (command.commentVisibleGroup != "All Users") {
            val groupJson = JsonObject()
            val visibilityJson = JsonObject()
            groupJson.addProperty("id", getGroupId(command))
            visibilityJson.addProperty("\$type", "CommandLimitedVisibility")
            visibilityJson.add("permittedGroups", JsonArray().also { it.add(groupJson) })
            json.add("visibility", visibilityJson)
        }
        postMethod.entity = json.toString().jsonEntity
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