package com.github.jk1.ytplugin.rest

import com.github.jk1.ytplugin.issues.model.Issue
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.minidev.json.JSONObject
import org.apache.commons.httpclient.NameValuePair
import org.apache.commons.httpclient.methods.GetMethod
import org.apache.commons.httpclient.methods.PostMethod
import org.apache.commons.httpclient.methods.StringRequestEntity
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets


/**
 * Fetches YouTrack issues with issue description formatted from wiki into html on server side.
 */
class IssuesRestClient(override val repository: YouTrackServer) : IssuesRestClientBase, RestClientTrait {

    override fun createDraft(summary: String): String {
        val method = PostMethod("${repository.url}/api/admin/users/me/drafts")
        //todo: force markdown
        method.requestEntity = StringRequestEntity(summary.asMarkdownIssueDraft(), "application/json", StandardCharsets.UTF_8.name())
        return method.connect {
            val status = httpClient.executeMethod(method)
            if (status == 200) {
                val stream = InputStreamReader(method.responseBodyAsLoggedStream(), "UTF-8")
                JsonParser().parse(stream).asJsonObject.get("id").asString
            } else {
                throw RuntimeException(method.responseBodyAsLoggedString())
            }
        }
    }

    override fun getIssue(id: String): Issue? {
        val method = GetMethod("${repository.url}/api/issues/$id")
        val fields = NameValuePair("fields", "id,idReadable,updated,created," +
                "tags(color(foreground,background),name),project,links(value,direction,issues(idReadable)," +
                "linkType(name,sourceToTarget,targetToSource),id),comments(id,text,created,updated," +
                "author(name,%20authorFullName,login)),summary,wikifiedDescription,customFields(name,value(name)," +
                "id,projectCustomField),resolved,attachments(name,url),description,reporter(login)")
//        val fields = NameValuePair("fields", "id,idReadable,summary,description,customFields(id,name,value(id,name)))")
        method.setQueryString(arrayOf(fields))
        return method.execute { IssueJsonParser.parseIssue(it, repository.url) }
    }

    private fun parseIssues(method: GetMethod): MutableList<Issue>{
        println("hey1")

        return method.connect {
            val list: MutableList<Issue> = mutableListOf()

            val status = httpClient.executeMethod(method)
            println("hey2")
            val json: JsonArray = JsonParser().parse(method.responseBodyAsString) as JsonArray
            println("hey3")
            for (i in 0 until json.size()) {
                val e: JsonObject = json.get(i) as JsonObject
                println("hey4 $e")
                println("my id " + e.get("idReadable").asString)

                val currentIssue = IssueParser().parseIssue(e, method.uri.toString())
                println("id: " + currentIssue.id)
                println("entityId: " + currentIssue.entityId)
                list.add(currentIssue)
            }
            println("status: $status")
            if (status == 200) {
                list
            }  else {
                throw RuntimeException(method.responseBodyAsLoggedString())
            }
        }
    }

    override fun getIssues(query: String): List<Issue> {
        // todo: customizable "max" limit
        val url = "${repository.url}/api/issues"
        val method = GetMethod(url)
        val myQuery = NameValuePair("query", query)
        val myFields = NameValuePair("fields", "id,idReadable,updated,created," +
                "tags(color(foreground,background),name),project,links(value,direction,issues(idReadable)," +
                "linkType(name,sourceToTarget,targetToSource),id),comments(id,text,created,updated," +
                "author(name,%20authorFullName,login)),summary,wikifiedDescription,customFields(name,value(name)," +
                "id,projectCustomField),resolved,attachments(name,url),description,reporter(login)")

        method.setQueryString(arrayOf(myQuery, myFields))
        return parseIssues(method)
    }

    fun String.asMarkdownIssueDraft() = JSONObject().also {
        it["description"] = this
        it["usesMarkdown"] = true
    }.toJSONString()
}