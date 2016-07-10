package com.github.jk1.ytplugin.search.model

import com.google.gson.JsonElement

class Issue(item: JsonElement) {

    val id: String
    val summary: String
    val description: String

    init {
        id = ""
        summary = ""
        description = ""
    }
}