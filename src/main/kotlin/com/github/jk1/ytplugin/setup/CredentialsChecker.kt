package com.github.jk1.ytplugin.setup

import com.intellij.openapi.components.Service

@Service
class CredentialsChecker {

    private val tokenPattern = Regex("perm:([^.]+)\\.([^.]+)\\.(.+)")
    private val appPasswordPattern = Regex("\\w{20}")
    private val appPasswordPatternWithDashes = Regex("\\w{4}-\\w{4}-\\w{4}-\\w{4}-\\w{4}")


    fun isMatchingBearerToken(token: String): Boolean {
        return token.matches(tokenPattern)
    }

    fun isMatchingAppPassword(token: String): Boolean {
        val password = token.split(Regex(":"), 2).last()
        return password.matches(appPasswordPattern) || password.matches(appPasswordPatternWithDashes)
    }

}