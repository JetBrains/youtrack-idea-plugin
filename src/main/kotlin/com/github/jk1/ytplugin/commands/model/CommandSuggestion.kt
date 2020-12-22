package com.github.jk1.ytplugin.commands.model

import com.github.jk1.ytplugin.logger
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.intellij.openapi.util.TextRange

class CommandSuggestion(item: JsonElement) {

    private val matchRange: TextRange
    private val completionRange: TextRange
    private val caretPosition: Int
    val description: String
    val suffix: String
    val prefix: String
    val option: String
    val separator: Boolean

    init {
        matchRange = try {
            TextRange(
                    item.asJsonObject.get("matchingStart").asInt,
                    item.asJsonObject.get("matchingEnd").asInt
            )
        } catch (e: IllegalArgumentException) {
            logger.warn("Invalid matching: ${e.message}")
            TextRange(
                    0,
                    item.asJsonObject.get("matchingEnd").asInt
            )

        }
        completionRange = TextRange(
                item.asJsonObject.get("completionStart").asInt,
                item.asJsonObject.get("completionEnd").asInt
        )
        description = item.asJsonObject.get("description").asStringNullSafe()
        option = item.asJsonObject.get("option").asStringNullSafe()
        suffix = item.asJsonObject.get("suffix").asStringNullSafe()
        prefix = item.asJsonObject.get("prefix").asStringNullSafe()
        caretPosition = item.asJsonObject.get("caret").asInt

        //TODO: check if separator is needed
        separator = false

    }

    private fun JsonElement.asStringNullSafe(default: String = ""): String = when (this) {
        is JsonNull -> default
        else -> this.asString
    }
}
