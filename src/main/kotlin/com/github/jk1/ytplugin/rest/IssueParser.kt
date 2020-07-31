package com.github.jk1.ytplugin.rest
import com.github.jk1.ytplugin.issues.model.*
import com.github.jk1.ytplugin.rest.IssueJsonParser
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import java.util.*

class IssueParser(){

    fun parseIssue(item: JsonElement, repoUrl: String): Issue{
        println("i am in parser1")

        val issue = Issue(item, repoUrl)
        println("i am in parser2")

        val root = item.asJsonObject
        println("i am in parser3")

        issue.id = root.get("idReadable").asString
        println("i am in parser" + issue.id)

        return issue
    }

    private fun JsonObject.getFieldValue(name: String): JsonElement? {
        return this.getAsJsonArray("field").firstOrNull {
            name == it.asJsonObject.get("name")?.asString
        }?.asJsonObject?.get("value")
    }

    private fun JsonElement.isCustomField(): Boolean {
        val name = asJsonObject.get("name")
        return name != null && !Issue.PREDEFINED_FIELDS.contains(name.asString)
    }
}


