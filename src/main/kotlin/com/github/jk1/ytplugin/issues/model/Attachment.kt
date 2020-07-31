package com.github.jk1.ytplugin.issues.model

import com.google.gson.JsonElement

class Attachment(item: JsonElement) {

    val fileName: String = item.asJsonObject.get("name").asString
    val url: String = item.asJsonObject.get("url").asString
}