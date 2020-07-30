package com.github.jk1.ytplugin.rest

import com.github.jk1.ytplugin.issues.model.Issue
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.minidev.json.JSONArray
import net.minidev.json.JSONObject
import org.apache.commons.httpclient.NameValuePair
import org.apache.commons.httpclient.methods.GetMethod
import org.apache.commons.httpclient.methods.PostMethod
import org.apache.commons.httpclient.methods.StringRequestEntity
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.*


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
        val fields = NameValuePair("fields", "id,idReadable,summary,description,customFields(id,name,value(id,name)))")
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
                println("hey4$e")
                println(e.get("idReadable").asString)
                println(e.get("description").asString)

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
//
//        for (i in 0 until issues.size()) {
//            val e: JsonObject = issues.get(i) as JsonObject
//            val currentIssue = Issue(e, method.uri.toString())
//            list.add(currentIssue)
//            println("hey2 $i")
//            println("id" + currentIssue.id)
//            println("entityId" + currentIssue.entityId)
//            println("summary " + currentIssue.summary)
//            println("desc " + currentIssue.description)
//            println( "crDate " + currentIssue.createDate)
//            println("upDate" + currentIssue.updateDate)
//            println("reso " + currentIssue.resolved)
//            println( "cf " + currentIssue.customFields)
//            println( "comm" + currentIssue.comments[0])
//            println( "link" + currentIssue.links[0])
//            println( "tag" + currentIssue.tags[0])
//            println("att " + currentIssue.attachments)
//            println( "url " + currentIssue.url)
//            println("wiki" + currentIssue.wikified)
//
//        }
//        return list
    }

    override fun getIssues(query: String): List<Issue> {
        // todo: customizable "max" limit
        val url = "${repository.url}/api/issues?query=${query.urlencoded}"
        val method = GetMethod(url)

        val fields = NameValuePair("fields", "id,idReadable,updated,created," +
                "tags(color(foreground,background),name),project,links,comments(id,text,created,updated," +
                "author(name,%20authorFullName,login)),summary,wikifiedDescription,customFields(name,value(name)," +
                "id,projectCustomField),resolved,attachments,description,reporter(login)")

        method.setQueryString(arrayOf(fields))
        val issues: MutableList<Issue>  = parseIssues(method)

//        val issues = GetMethod(url).execute {
//            it.asJsonObject.getAsJsonArray("issue").mapNotNull { IssueJsonParser.parseIssue(it, repository.url) }
//        }

        if (issues.any { it.wikified }) {
            // this is YouTrack 2018.1+, so we can return wikified issues right away
            return issues
        } else {
            /*
             * There's no direct API to get formatted issues by a search query, so two-stage fetch is used:
             * - Fetch issues by search query and select all projects these issues belong to
             * - For each project request formatted issues with an auxiliary search request like
             * "issue id: PR-1 or issue id: PR-2 or ...". Auxiliary source request is necessary
             *  due to https://github.com/jk1/youtrack-idea-plugin/issues/30
             * - Sort issues to match the order from the first request
             */
            val issuesIds = issues.map { it.id }
            val projects = issuesIds.groupBy { it.split("-")[0] }
            val wikifiedIssues = projects.flatMap {
                val issueIdsQuery = it.value.joinToString(" ") { "#$it" }
                getWikifiedIssuesInProject(it.key, issueIdsQuery)
            }
            return issuesIds.mapNotNull { id -> wikifiedIssues.firstOrNull { issue -> id == issue.id } }
        }
    }

    override fun getWikifiedIssuesInProject(projectShortName: String, query: String): List<Issue> {
        val url = "${repository.url}/rest/issue/byproject/${projectShortName.urlencoded}"
        val params = "filter=${query.urlencoded}&wikifyDescription=true&max=30"
        val method = GetMethod("$url?$params")
        return method.execute { it.asJsonArray.mapNotNull { IssueJsonParser.parseIssue(it, repository.url) } }
    }

    fun String.asMarkdownIssueDraft() = JSONObject().also {
        it["description"] = this
        it["usesMarkdown"] = true
    }.toJSONString()
}