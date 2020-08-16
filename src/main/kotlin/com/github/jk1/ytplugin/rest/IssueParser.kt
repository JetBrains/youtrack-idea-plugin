package com.github.jk1.ytplugin.rest
<<<<<<< HEAD
import com.github.jk1.ytplugin.issues.model.*
import com.google.gson.JsonArray
=======
import com.github.jk1.ytplugin.issues.model.Issue
>>>>>>> 85cd14ab7604757104b84581df925c4fcb778484
import com.google.gson.JsonElement
import com.google.gson.JsonObject

class IssueParser(){

    fun parseIssue(item: JsonElement, repoUrl: String): Issue{
        val issue = Issue(item, repoUrl)
        val root = item.asJsonObject
        issue.id = root.get("idReadable").asString
        return issue
    }

}


