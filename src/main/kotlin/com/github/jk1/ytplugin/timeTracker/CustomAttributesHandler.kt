package com.github.jk1.ytplugin.timeTracker

import com.google.gson.JsonArray

class CustomAttributesHandler {

    fun parseCustomAttributes(json: JsonArray): Map<String, List<String>> {
        return if (json.isJsonNull || json.size() == 0){
            mapOf()
        } else {
            // TODO add try catch
            val valuesMap = mutableMapOf<String, List<String>>()
            json.forEach { it ->
                val valuesNames = json.first().asJsonObject.get("values").asJsonArray.map { it.asJsonObject.get("name").asString}
                valuesMap[it.asJsonObject.get("name").asString] = valuesNames
            }
            valuesMap
        }
    }

}