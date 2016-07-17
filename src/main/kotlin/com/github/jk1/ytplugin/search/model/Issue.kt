package com.github.jk1.ytplugin.search.model

import com.google.gson.JsonElement
import com.google.gson.JsonObject

class Issue(item: JsonElement) {

    val id: String
    val entityId: String
    val summary: String
    val description: String
    val customFields: Map<String, String> = mutableMapOf()

    init {
        val root = item.asJsonObject
        id = root.get("id").asString
        entityId = root.get("entityId").asString
        summary = getPredefinedFieldValue("summary", root)
        description = getPredefinedFieldValue("description", root)
    }

    private fun getPredefinedFieldValue(name: String, root: JsonObject): String {
        return root.getAsJsonArray("field").first {
            "summary".equals(it.asJsonObject.get("name").asString)
        }.asJsonObject.get("value").asString
    }
}