package com.github.jk1.ytplugin.notifications

import com.google.gson.JsonElement

class YouTrackNotification(item: JsonElement) {

        val text: String = "!"

    init {
        val root = item.asJsonObject
    }
}