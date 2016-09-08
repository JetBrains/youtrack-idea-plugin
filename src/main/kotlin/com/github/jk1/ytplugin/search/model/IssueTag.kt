package com.github.jk1.ytplugin.search.model

import com.google.gson.JsonElement

class IssueTag(item: JsonElement) {

    val value: String = item.asJsonObject.get("value").asString
}