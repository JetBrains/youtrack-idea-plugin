package com.github.jk1.ytplugin.rest

import com.github.jk1.ytplugin.issues.model.IssueWorkItem
import com.github.jk1.ytplugin.logger
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import org.apache.commons.httpclient.NameValuePair
import org.apache.commons.httpclient.methods.GetMethod


class UserRestClient(override val repository: YouTrackServer) : RestClientTrait {

    private fun parseWorkItems(method: GetMethod): MutableList<IssueWorkItem> {

        return method.connect {
            val status = httpClient.executeMethod(method)
            val json: JsonArray = JsonParser.parseString(method.responseBodyAsString) as JsonArray
            val workItems = mutableListOf<IssueWorkItem>()
            json.mapNotNull { workItems.add(IssueJsonParser.parseWorkItem(it)!!) }
            workItems.sort()
            if (status == 200) {
                logger.debug("Successfully parsed ${workItems.size} work items: $status")
                workItems
            } else {
                logger.debug("Failed to parse work items: $status, ${method.responseBodyAsLoggedString()}")
                throw RuntimeException(method.responseBodyAsLoggedString())
            }
        }
    }

    fun getWorkItemsForUser(): List<IssueWorkItem> {
        val myQuery = NameValuePair("query", "created by: me")
        val myFields = NameValuePair("fields", "text,issue(idReadable),type(name),created," +
                "duration(presentation,minutes),author(name),creator(name),date,id")

        val url = "${repository.url}/api/workItems"
        val method = GetMethod(url)
        method.setQueryString(arrayOf(myQuery, myFields))
        val sortedList = parseWorkItems(method)
                .sortedWith(compareByDescending { it.created })
                .sortedWith(compareByDescending { it.date })

        return sortedList
    }
}