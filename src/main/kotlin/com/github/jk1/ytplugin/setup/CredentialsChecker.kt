package com.github.jk1.ytplugin.setup

import com.google.gson.JsonParser
import com.intellij.openapi.components.Service
import org.apache.http.HttpEntity
import org.apache.http.util.EntityUtils

@Service
class CredentialsChecker {

    private val tokenPattern = Regex("perm:([^.]+)\\.([^.]+)\\.(.+)")
    private val appPasswordPattern = Regex("\\w{20}")
    private val appPasswordPatternWithDashes = Regex("\\w{4}-\\w{4}-\\w{4}-\\w{4}-\\w{4}")


    fun isGuestUser(response: HttpEntity): Boolean{
        val user = JsonParser.parseString(EntityUtils.toString(response, "UTF-8"))
            .asJsonObject.get("name").toString()
        return user == "\"guest\""
    }

    fun isMatchingBearerToken(token: String): Boolean {
        return token.matches(tokenPattern)
    }

    fun isMatchingAppPassword(token: String): Boolean {
        val password = token.split(Regex(":"), 2).last()
        return password.matches(appPasswordPattern) || password.matches(appPasswordPatternWithDashes)
    }

}