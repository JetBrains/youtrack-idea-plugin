package com.github.jk1.ytplugin

interface YouTrackConnectionTrait {

    val serverUrl: String
        get() = "https://ytplugintest.youtrack.cloud"

    val serverUrlOld: String
        get() = "https://ytplugintest.myjetbrains.com/youtrack"

    val projectId: String
        get() = "AT"

    val username: String
        get() = "ideplugin"

    val password: String
        get() = "ideplugin"

    val token: String
        get() = "perm:aWRlcGx1Z2lu.NjItMA==.7iaoaBCduVgrbAj9BkQSxksQLQcEte"

    val applicationPassword: String
        get() = "YOJSM4CNCD8ZKBSYI59H"

}