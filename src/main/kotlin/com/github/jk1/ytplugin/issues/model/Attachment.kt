package com.github.jk1.ytplugin.issues.model

import com.google.gson.JsonElement

class Attachment(item: JsonElement, repoUrl: String) {

    private val tmpUrl: String = item.asJsonObject.get("url").asString

    val fileName: String = item.asJsonObject.get("name").asString

    val url: String = repoUrl + if (tmpUrl.contains("/youtrack"))
        tmpUrl.split("/youtrack")[1] else tmpUrl
}