package com.github.jk1.ytplugin.rest

import com.github.jk1.ytplugin.commands.model.CommandAssistResponse
import com.github.jk1.ytplugin.commands.model.CommandExecutionResponse
import com.github.jk1.ytplugin.commands.model.YouTrackCommand
import com.github.jk1.ytplugin.commands.model.YouTrackCommandExecution
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.google.gson.JsonParser
import com.intellij.javascript.debugger.execution.startSession
import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.methods.PostMethod
import org.apache.commons.httpclient.methods.StringRequestEntity
import org.jdom.input.SAXBuilder
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths


class CommandRestClient(override val repository: YouTrackServer) : RestClientTrait, ResponseLoggerTrait {


//    fun executeCommand(command: YouTrackCommandExecution): CommandExecutionResponse {
//        val execUrl = "${repository.url}api/commands"
//        val fields = NameValuePair("fields", "issues(id,idReadable),query,visibility(permittedGroups(id,name),permittedUsers(id,login))")
//
//
//        val method = PostMethod(execUrl)
//        method.setQueryString(arrayOf(fields))
//
//        val res: URL? = javaClass.classLoader.getResource("command_exe.json")
//        val absolutePath= Paths.get(res!!.toURI()).toFile().absolutePath
//        val jsonBody = command.executeCommandJson
//
//        println("JSON COMMAND: " + jsonBody)
//
//        method.requestEntity = StringRequestEntity(jsonBody, "application/json", StandardCharsets.UTF_8.name())
//
//        return method.connect {
//            it.addRequestHeader("Accept", "application/json")
//            val status = httpClient.executeMethod(method)
//            if (status != 200) {
//                val body = method.responseBodyAsLoggedStream()
//                when (method.getResponseHeader("Content-Type")?.value?.split(";")?.first()) {
//                    "application/xml" -> {
//                        val element = SAXBuilder(false).build(body).rootElement
//                        if ("error" == element.name) {
//                            CommandExecutionResponse(errors = listOf(element.text))
//                        } else {
//                            CommandExecutionResponse(messages = listOf(element.text))
//                        }
//                    }
//                    "application/json" -> {
//                        val stream = InputStreamReader(body, "UTF-8")
//                        val error = JsonParser().parse(stream).asJsonObject.get("value").asString
//                        CommandExecutionResponse(errors = listOf("Workflow: $error"))
//                    }
//                    else ->
//                        CommandExecutionResponse(errors = listOf("Unexpected command response from YouTrack server"))
//                }
//            } else {
//                method.responseBodyAsLoggedString()
//                CommandExecutionResponse()
//            }
//        }
//    }
////
//    private val YouTrackCommandExecution.executeCommandJson: String
//        get () {
//            val res: URL? = javaClass.classLoader.getResource("command_exe.json")
//            val absolutePath= Paths.get(res!!.toURI()).toFile().absolutePath
//            println ("SESSION ID: " + session.issue.id)
//            val jsonBody = String(Files.readAllBytes(Paths.get(absolutePath)))
//            jsonBody.apply {
//                replace("{query}", command.toString().urlencoded, true)
//                replace("{session.issue.id}", session.issue.id.toString().urlencoded, true)
//            }
//            return jsonBody
//        }


    fun assistCommand(command: YouTrackCommand): CommandAssistResponse {

        val client = HttpClient()
        val method = PostMethod("${repository.url}/api/commands/assist?fields=caret,commands%28delete,description,error%29,query,styleRanges%28end,length,start,style%29,suggestions%28caret,className,comment,completionEnd,completionStart,description,group,icon,id,matchingEnd,matchingStart,option,prefix,suffix%29")
        val caret = command.caret - 1

        val jsonBody = "{\n" +
                "  \"query\": \"${command.command}\",\n" +
                "  \"caret\": ${caret},\n" +
                "  \"issues\": [\n" +
                "    {\n" +
                "      \"idReadable\": \"TP-2\"\n" +
                "    }\n" +
                "  ]\n" +
                "}\n"
        method.setRequestHeader("Authorization", "Bearer " + repository.password)
        method.requestEntity = StringRequestEntity(jsonBody, "application/json", "UTF-8")
        val status = client.executeMethod(method)

//        val responseBody = method.responseBody
//        println(String(responseBody, StandardCharsets.UTF_8))


        return method.connect {
//            println("assistCommand HELLO2")
            if (status == 200) {
                CommandAssistResponse(method.responseBodyAsLoggedStream())
            } else {
                throw RuntimeException("HTTP $status: ${method.responseBodyAsLoggedString()}")
            }
        }
    }

    fun executeCommand(command: YouTrackCommandExecution): CommandExecutionResponse {
        val myField = "issues(id,idReadable),query,visibility(permittedGroups(id,name),permittedUsers(id,login))".urlencoded
//        val method = PostMethod("${repository.url}/api/commands?fields=issues%28id%2CidReadable%29%2Cquery%2Cvisibility%28permittedGroups%28id%2Cname%29%2CpermittedUsers%28id%2Clogin%29%29%0D%0A")
        val method = PostMethod("${repository.url}/api/commands?fields=${myField}")


        println("vis: " + command.session.issue.entityId)
        println("url: " + method.uri)
        println("id: " + command.session.issue.id)


        val jsonBody = "{\n" +
                "  \"query\": \"${command.command} \",\n" +
                "  \"issues\": [\n" +
                "    {\n" +
                "      \"idReadable\": \"${command.session.issue.id}\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"silent\": ${command.silent},\n" +
                "  \"comment\": \"${command.comment}\",\n" +
                "  \"visibility\": {\n" +
                "    \"\$type\": \"CommandLimitedVisibility\",\n" +
                "    \"permittedGroups\": [\n" +
                "      {\n" +
                "        \"id\": \"${command.session.issue.entityId}\"\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}"

        method.setRequestHeader("Authorization", "Bearer " + repository.password)
        method.requestEntity = StringRequestEntity(jsonBody, "application/json", "UTF-8")

        println("HELLO HEERE" )
        return method.connect {
            val status = httpClient.executeMethod(method)

            if (status != 200) {
                val body = method.responseBodyAsLoggedStream()
                when (method.getResponseHeader("Content-Type")?.value?.split(";")?.first()) {
                    "application/xml" -> {
                        val element = SAXBuilder(false).build(body).rootElement
                        if ("error" == element.name) {
                            CommandExecutionResponse(errors = listOf(element.text))
                        } else {
                            CommandExecutionResponse(messages = listOf(element.text))
                        }
                    }
                    "application/json" -> {
                        val stream = InputStreamReader(body, "UTF-8")
                        val error = JsonParser().parse(stream).asJsonObject.get("value").asString
                        CommandExecutionResponse(errors = listOf("Workflow: $error"))
                    }
                    else ->
                        CommandExecutionResponse(errors = listOf("Unexpected command response from YouTrack server"))
                }
            } else {
                method.responseBodyAsLoggedString()
                CommandExecutionResponse()
            }
        }
    }

    private val YouTrackCommandExecution.executeCommandUrl: String
        get () {
            val execUrl = "${repository.url}/rest/issue/execute/${session.issue.id}"
            var params = "command=${command.urlencoded}&comment=${comment?.urlencoded}&disableNotifications=$silent"
            if (commentVisibleGroup != "All Users") {
                // 'All Users' shouldn't be passed as a parameter value. Localized YouTracks can't understand that.
                params = "$params&group=${commentVisibleGroup.urlencoded}"
            }
            return "$execUrl?$params"
        }

    private val YouTrackCommand.getCommandJson: String
        get () {
//            val res = javaClass.classLoader.getResource("admin_body.json")
//            val file: File = Paths.get(res.toURI()).toFile()
//            val absolutePath: String = file.absolutePath
//            println("PATH COMMAND:  " + absolutePath)

            val jsonBody = String(Files.readAllBytes(
                    Paths.get("/home/lesslyrics/WorkJB/youtrack-idea-plugin/build/resources/main/get_command_body.json"))).replace("{id}", session.issue.id, true)
            println ("SESSION ID: " + session.issue.id)
            println("res body " + jsonBody.toString())
            return jsonBody
        }

}