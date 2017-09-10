package com.github.jk1.ytplugin.commands.model

import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.intellij.openapi.util.TextRange


class CommandHighlightRange(rangeElement: JsonElement) {

    private val start: Int = rangeElement.asJsonObject.get("start").asInt
    private val end: Int = rangeElement.asJsonObject.get("end").asInt
    val styleClass = rangeElement.asJsonObject.get("style").asStringNullSafe()

    fun getTextRange() = TextRange.create(start, end)

    private fun JsonElement.asStringNullSafe(default: String = ""): String = when (this) {
        is JsonNull -> default
        else -> this.asString
    }
}