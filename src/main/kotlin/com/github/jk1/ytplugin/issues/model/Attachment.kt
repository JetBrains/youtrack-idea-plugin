package com.github.jk1.ytplugin.issues.model

import com.google.gson.JsonElement

class Attachment(item: JsonElement, repoUrl: String) {

    val fileName: String = item.asJsonObject.get("name").asString

    val url: String =
            "$repoUrl/api/files/${item.asJsonObject.get("url").asString.split("/api/files/").last()}"
}