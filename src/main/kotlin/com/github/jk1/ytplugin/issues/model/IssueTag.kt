package com.github.jk1.ytplugin.issues.model

import com.github.jk1.ytplugin.asColor
import com.google.gson.JsonElement
import java.awt.Color

class IssueTag(item: JsonElement) {

    val text: String
    val foregroundColor: Color
    val backgroundColor: Color

    init {
        val fgColor = item.asJsonObject.get("color").asJsonObject.get("foreground")
        val bgColor = item.asJsonObject.get("color").asJsonObject.get("background")
        foregroundColor = fgColor.asColor()
        backgroundColor = bgColor.asColor()
        text = item.asJsonObject.get("name").asString
    }
}