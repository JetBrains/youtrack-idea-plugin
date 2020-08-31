package com.github.jk1.ytplugin.rest

import com.github.jk1.ytplugin.issues.model.Issue
import com.github.jk1.ytplugin.issues.model.IssueWorkItem
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import org.apache.commons.httpclient.NameValuePair
import org.apache.commons.httpclient.methods.GetMethod
import org.apache.commons.httpclient.methods.PostMethod
import org.apache.commons.httpclient.methods.StringRequestEntity
import sun.security.ec.point.ProjectivePoint
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets


/**
 * Fetches YouTrack issues with issue description formatted from wiki into html on server side.
 */
class UserRestClient(override val repository: YouTrackServer) : RestClientTrait {

    private fun parseWorkItems(method: GetMethod): MutableList<IssueWorkItem> {

        return method.connect {
            val status = httpClient.executeMethod(method)
            val json: JsonArray = JsonParser.parseString(method.responseBodyAsString) as JsonArray
            val workItems = mutableListOf<IssueWorkItem>()
            json.mapNotNull { workItems.add(IssueJsonParser.parseWorkItem(it)!!) }
            workItems.sort()

            if (status == 200) {
                workItems
            } else {
                throw RuntimeException(method.responseBodyAsLoggedString())
            }
        }
    }

    fun getWorkItemsForUser(query: String): List<IssueWorkItem> {
        val myQuery = NameValuePair("query", query)
        val url = "${repository.url}/api/workItems"
        val method = GetMethod(url)
        val myFields = NameValuePair("fields", "text,issue(idReadable),type(name),created," +
                "duration(presentation,minutes),author(name),creator(name),date,id")
        method.setQueryString(arrayOf(myQuery, myFields))

        val sortedList = parseWorkItems(method)
                .sortedWith(compareByDescending { it.created })
                .sortedWith(compareByDescending { it.date })

        return sortedList
    }
}