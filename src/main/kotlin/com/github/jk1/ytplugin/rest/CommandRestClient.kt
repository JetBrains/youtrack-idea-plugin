package com.github.jk1.ytplugin.rest

import com.github.jk1.ytplugin.commands.model.CommandAssistResponse
import com.github.jk1.ytplugin.commands.model.CommandExecutionResponse
import com.github.jk1.ytplugin.commands.model.YouTrackCommand
import com.github.jk1.ytplugin.commands.model.YouTrackCommandExecution
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import org.apache.commons.httpclient.NameValuePair
import org.apache.commons.httpclient.methods.GetMethod
import org.apache.commons.httpclient.methods.PostMethod
import org.apache.commons.httpclient.methods.StringRequestEntity
import java.io.InputStreamReader
import java.net.URL


class CommandRestClient(override val repository: YouTrackServer) : CommandRestClientBase, RestClientTrait, ResponseLoggerTrait {

    override fun assistCommand(command: YouTrackCommand): CommandAssistResponse {
        val method = PostMethod("${repository.url}/api/commands/assist")
        val fields = NameValuePair("fields", "commands(description,error),styleRanges(length,start,style)," +
                "suggestions(caret,completionEnd,completionStart,description,matchingEnd,matchingStart,option,prefix,suffix)")
        method.setQueryString(arrayOf(fields))
        val caret = command.caret - 1
        val res: URL? = this::class.java.classLoader.getResource("get_command_body.json")
        val id = command.session.issue.id
        val jsonBody = res?.readText()
                ?.replace("{query}", command.command, true)
                ?.replace("0", caret.toString(), true)
                ?.replace("{id}", id, true)

        method.requestEntity = StringRequestEntity(jsonBody, "application/json", "UTF-8")

        return method.connect {
            val status = httpClient.executeMethod(method)
            if (status == 200) {
                CommandAssistResponse(method.responseBodyAsLoggedStream())
            } else {
                throw RuntimeException("HTTP $status: ${method.responseBodyAsLoggedString()}")
            }
        }
    }

    private fun getGroupId(command: YouTrackCommandExecution): String {
        val execUrl = "${repository.url}/api/groups?fields=name,id"
        val getMethod = GetMethod(execUrl)
        val status = httpClient.executeMethod(getMethod)
        val response: JsonArray = JsonParser.parseString(getMethod.responseBodyAsString) as JsonArray
        var groupId: String = ""
        if (status == 200) {
            for (element in response) {
                if (command.commentVisibleGroup == element.asJsonObject.get("name").asString)
                    groupId = element.asJsonObject.get("id").asString
            }
        }
        return groupId
    }

    override fun executeCommand(command: YouTrackCommandExecution): CommandExecutionResponse {
        val groupId: String = getGroupId(command)
        val execPostUrl = "${repository.url}/api/commands"
        val postMethod = PostMethod(execPostUrl)

        val comment = command.comment ?: ""

        val res: URL? = this::class.java.classLoader.getResource("command_execution_rest.json")
        val jsonBody = res?.readText()?.replace("{groupId}", groupId, true)
                ?.replace("{idReadable}", command.session.issue.id, true)
                ?.replace("true", command.silent.toString(), true)
                ?.replace("{comment}", comment, true)
                ?.replace("{query}", command.command, true)

        postMethod.requestEntity = StringRequestEntity(jsonBody, "application/json", "UTF-8")
        return postMethod.connect {
            val status = httpClient.executeMethod(postMethod)
            if (status != 200) {
                val body = postMethod.responseBodyAsLoggedStream()
                val error = JsonParser.parseReader(InputStreamReader(body, "UTF-8")).asJsonObject.get("value").asString
                CommandExecutionResponse(errors = listOf("Workflow: $error"))
            } else {
                postMethod.responseBodyAsLoggedString()
                CommandExecutionResponse()
            }
        }
    }
}