package com.github.jk1.ytplugin.commands.model

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
        matchRange = TextRange(
                item.asJsonObject.get("ms").asInt,
                item.asJsonObject.get("me").asInt
        )
        completionRange = TextRange(
                item.asJsonObject.get("cs").asInt,
                item.asJsonObject.get("ce").asInt
        )
        description = item.asJsonObject.get("d").asStringNullSafe()
        option = item.asJsonObject.get("o").asStringNullSafe()
        suffix = item.asJsonObject.get("suf").asStringNullSafe()
        prefix = item.asJsonObject.get("pre").asStringNullSafe()
        caretPosition = item.asJsonObject.get("cp").asInt
        separator = item.asJsonObject.get("sep").asBoolean
    }

    private fun JsonElement.asStringNullSafe(default: String = ""): String = when (this) {
        is JsonNull -> default
        else -> this.asString
    }
}
