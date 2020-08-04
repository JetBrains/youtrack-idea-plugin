package com.github.jk1.ytplugin.rest

import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.apache.commons.httpclient.NameValuePair
import org.apache.commons.httpclient.methods.GetMethod
import org.apache.commons.httpclient.methods.PostMethod
import org.apache.commons.httpclient.methods.StringRequestEntity
import java.nio.charset.StandardCharsets

class AdminRestClient(override val repository: YouTrackServer) : AdminRestClientBase, RestClientTrait, ResponseLoggerTrait {

    override fun getVisibilityGroups(issueId: String): List<String> {
        val getGroupsUrl = "${repository.url}/api/visibilityGroups"
        val method = PostMethod(getGroupsUrl)
        method.params.contentCharset = "UTF-8"

        val top = NameValuePair("top", "-1")
        val fields = NameValuePair("fields", "groupsWithoutRecommended(name),recommendedGroups(name)")

        method.setQueryString(arrayOf(top, fields))
        val jsonBody = "{\"issues\":[{\"type\":\"Issue\",\"id\":\"${issueId}\"}],\"prefix\":\"\",\"top\":20}"
        method.requestEntity = StringRequestEntity(jsonBody, "application/json", StandardCharsets.UTF_8.name())

        return method.connect {
            val status = httpClient.executeMethod(method)
            val groups: MutableList<String>  = mutableListOf("All Users")
            parseGroups(groups, method, "recommendedGroups")
            parseGroups(groups, method, "groupsWithoutRecommended")
            when (status) {
                // YouTrack 5.2 has no rest method to get visibility groups{
                200, 404 -> {
                    groups
                }
                else -> throw RuntimeException(method.responseBodyAsLoggedString())
            }
        }
    }


    private fun parseGroups(list: MutableList<String>, method: PostMethod, elem: String){
        val myObject: JsonObject = JsonParser.parseString(method.responseBodyAsString) as JsonObject
        val recommendedGroups: JsonArray = myObject.get(elem) as JsonArray
        for (i in 0 until recommendedGroups.size()) {
            val recommendedGroup: JsonObject = recommendedGroups.get(i) as JsonObject
            list.add(recommendedGroup.get("name").asString)
        }
    }

    override fun getAccessibleProjects(): List<String> {
        val method = GetMethod("${repository.url}/api/admin/projects")
        val fields = NameValuePair("fields", "shortName")
        method.setQueryString(arrayOf(fields))

        return method.connect {
            val status = httpClient.executeMethod(method)
            val shortNamesList: MutableList<String>  = mutableListOf()

            val json: JsonArray = JsonParser.parseString(method.responseBodyAsString) as JsonArray
            for (i in 0 until json.size()) {
                val e: JsonObject = json.get(i) as JsonObject
                shortNamesList.add(e.get("shortName").asString)
            }
            if (status == 200) {
                shortNamesList
            }  else {
                throw RuntimeException(method.responseBodyAsLoggedString())
            }
        }
    }
}