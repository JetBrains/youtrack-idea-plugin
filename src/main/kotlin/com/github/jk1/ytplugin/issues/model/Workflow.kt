package com.github.jk1.ytplugin.issues.model

import com.google.gson.JsonArray
import com.google.gson.JsonElement


class Workflow(item: JsonElement) {

    val name: String
    val id: String
    var rules: MutableList<WorkflowRule> = mutableListOf()

    init {
        val root = item.asJsonObject
        id = root.get("id").asString
        name = root.get("name").asString
        for (rule in (root.get("rules") as JsonArray)) {
            rules.add(WorkflowRule(rule))
        }
    }
}


