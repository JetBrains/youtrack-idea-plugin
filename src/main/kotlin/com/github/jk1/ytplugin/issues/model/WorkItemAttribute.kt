package com.github.jk1.ytplugin.issues.model

import com.github.jk1.ytplugin.logger
import com.google.gson.JsonElement

class WorkItemAttribute(item: JsonElement) {
    var json: String
    var id: String
    val name: String
    val value: String?

    init {
        val root = item.asJsonObject
        json = item.toString()
        id = root.get("id").asString
        name = root.get("name").asString
        value =  try {
            root.get("value").asJsonObject.get("name").asString
        } catch (e: IllegalStateException) {
            logger.trace("empty attribute: ${e.message}")
            null
        }
    }
}