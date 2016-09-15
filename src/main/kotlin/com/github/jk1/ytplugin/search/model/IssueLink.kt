package com.github.jk1.ytplugin.search.model

import com.google.gson.JsonElement

class IssueLink(item: JsonElement, repoUrl: String) {

    val type: String = item.asJsonObject.get("type").asString
    val role: String = item.asJsonObject.get("role").asString
    val value: String = item.asJsonObject.get("value").asString
    val url = "$repoUrl/issue/$value"
}