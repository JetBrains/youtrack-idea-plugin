package com.github.jk1.ytplugin.setup

import com.intellij.openapi.components.Service

@Service
class CredentialsChecker {

    private val tokenPattern = Regex("perm:([^.]+)\\.([^.]+)\\.(.+)")
    private val appPasswordPattern = Regex("\\w{20}")

    fun isMatchingBearerToken(token: String): Boolean {
        return token.matches(tokenPattern)
    }

    //TODO helper for XXXX-XXXX pass format error
    fun isMatchingAppPassword(token: String): Boolean {
        return token.split(Regex(":"), 2).last().matches(appPasswordPattern)
    }

}