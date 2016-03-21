package com.github.jk1.ytplugin.commands.model

import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.intellij.openapi.util.TextRange


class CommandHighlightRange(rangeElement: JsonElement) {

    val start: Int
    val end: Int
    val styleClass: String

    init {
        start = rangeElement.asJsonObject.get("start").asInt
        end = rangeElement.asJsonObject.get("end").asInt
        styleClass = rangeElement.asJsonObject.get("style").asStringNullSafe()
    }

    fun getRange() = TextRange(start, end)

    fun getTextRange() = TextRange.create(start, end)

    fun JsonElement.asStringNullSafe(default: String = ""): String = when (this) {
        is JsonNull -> default
        else -> this.asString
    }
}