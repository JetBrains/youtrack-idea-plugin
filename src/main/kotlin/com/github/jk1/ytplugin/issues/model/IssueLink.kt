package com.github.jk1.ytplugin.issues.model

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject

class IssueLink(item: JsonElement, repoUrl: String, index: Int) {

    var type: String = String()
    var value: String = String()
    var role: String = String()
    var url: String  = String()

    init {
        var i = index
        val issues: JsonElement = item.asJsonObject.get("issues")
        if ((issues.isJsonArray && issues.asJsonArray.size() != 0)) {
            if (index < issues.asJsonArray.size()){
                type = item.asJsonObject.get("linkType").asJsonObject.get("name").asString
                value = getValue(item, index)
                val direction = item.asJsonObject.get("direction").asString
                if (direction == "INWARD")
                    role = item.asJsonObject.get("linkType").asJsonObject.get("targetToSource").asString
                else if (direction == "OUTWARD" || direction == "BOTH")
                    role = item.asJsonObject.get("linkType").asJsonObject.get("sourceToTarget").asString
                i++
                IssueLink(item, repoUrl, i)
            }
            url = "$repoUrl/issue/$value"
        }
    }

    fun getValue(item: JsonElement,index: Int): String {
            return item.asJsonObject.get("issues").asJsonArray[index].asJsonObject.get("idReadable").asString
    }

}