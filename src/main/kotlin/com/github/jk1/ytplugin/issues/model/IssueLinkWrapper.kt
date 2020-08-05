package com.github.jk1.ytplugin.issues.model

import com.google.gson.JsonElement


class IssueLinkWrapper() {

    var type: String = String()
    var value: String = String()
    var role: String = String()
    var url: String  = String()

    fun reformatIssues(item: JsonElement, repoUrl: String) : MutableList<IssueLink>{
        val issues: JsonElement = item.asJsonObject.get("issues")

        val result:MutableList<IssueLink> = mutableListOf()
        if ((issues.isJsonArray && issues.asJsonArray.size() != 0)) {

            type = item.asJsonObject.get("linkType").asJsonObject.get("name").asString

            val direction = item.asJsonObject.get("direction").asString
            if (direction == "INWARD")
                role = item.asJsonObject.get("linkType").asJsonObject.get("targetToSource").asString
            else if (direction == "OUTWARD" || direction == "BOTH")
                role = item.asJsonObject.get("linkType").asJsonObject.get("sourceToTarget").asString

            val myLinks =item.asJsonObject.get("issues").asJsonArray
             for (element in myLinks){
                 value = element.asJsonObject.get("idReadable").asString
                 url = "$repoUrl/issue/$value"
                 result.add(IssueLink(value, type, role, url))
             }
        }
        return result
    }

}