package com.github.jk1.ytplugin.commands.model

import com.google.gson.JsonElement


class CommandPreview(commandElement: JsonElement) {

    val description: String
    val error: Boolean

    init {
        description = commandElement.asJsonObject.get("desc").asString
        error = commandElement.asJsonObject.get("error").asBoolean
    }
}