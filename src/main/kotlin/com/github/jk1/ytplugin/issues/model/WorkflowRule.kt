package com.github.jk1.ytplugin.issues.model

import com.google.gson.JsonElement


class WorkflowRule(item: JsonElement) {

    val name: String
    val id: String
    var content: String = ""

    init {
        val root = item.asJsonObject
        name = root.get("name").asString
        id = root.get("id").asString
    }

}
