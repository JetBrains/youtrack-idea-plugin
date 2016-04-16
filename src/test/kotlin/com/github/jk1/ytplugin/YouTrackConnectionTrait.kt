package com.github.jk1.ytplugin

interface YouTrackConnectionTrait {

    val serverUrl: String
        get() = "https://ytplugintest.myjetbrains.com/youtrack"

    val projectId: String
        get() = "AT"

    val username: String
        get() = "ideplugin"

    val password: String
        get() = "ideplugin"
}