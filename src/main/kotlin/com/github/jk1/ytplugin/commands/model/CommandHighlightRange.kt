package com.github.jk1.ytplugin.commands.model

import com.github.jk1.ytplugin.logger
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.intellij.openapi.util.TextRange


class CommandHighlightRange(rangeElement: JsonElement) {

    private val start: Int = rangeElement.asJsonObject.get("start").asInt
    private val end: Int = start + rangeElement.asJsonObject.get("length").asInt - 1
    val styleClass = rangeElement.asJsonObject.get("style").asStringNullSafe()


    fun getTextRange() =
            try {
                TextRange.create(start, end)
            } catch (e: IllegalArgumentException){
                logger.debug("Exception in CommandHighlightRange: ${e.message}")
                TextRange.create(0, 0)
            }

    private fun JsonElement.asStringNullSafe(default: String = ""): String = when (this) {
        is JsonNull -> default
        else -> this.asString
    }
}