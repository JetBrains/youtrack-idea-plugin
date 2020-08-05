package com.github.jk1.ytplugin.issues.model

import com.google.gson.JsonElement
import java.awt.Color

class IssueTag(item: JsonElement) {

    val text: String
    val foregroundColor: Color
    val backgroundColor: Color
    private val borderColor: Color

    init {
        val fgColor = item.asJsonObject.get("color").asJsonObject.get("foreground").asString
        val bgColor = item.asJsonObject.get("color").asJsonObject.get("background").asString
        val tagColor = TagColor(fgColor, bgColor)
        foregroundColor = tagColor.foreground
        backgroundColor = tagColor.background
        borderColor = tagColor.foreground
        text = item.asJsonObject.get("name").asString
    }

    class TagColor(foreground: String, background: String) {
        val foreground: Color = Color.decode(foreground)
        val background: Color = Color.decode(background)
    }
}