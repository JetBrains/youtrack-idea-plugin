package com.github.jk1.ytplugin.issues.model

import com.google.gson.JsonElement

class Attachment(item: JsonElement, repoUrl: String) {
   init{
       println("LINK TO URL HERE: " + repoUrl)
   }

    val fileName: String = item.asJsonObject.get("name").asString
    val url: String = repoUrl.substring(0, repoUrl.length - 9) + item.asJsonObject.get("url").asString
}