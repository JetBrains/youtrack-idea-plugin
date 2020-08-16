package com.github.jk1.ytplugin.rest

import com.github.jk1.ytplugin.issues.model.Issue
import com.google.gson.JsonElement

class IssueParser(){

    fun parseIssue(item: JsonElement, repoUrl: String): Issue{
        val issue = Issue(item, repoUrl)
        val root = item.asJsonObject
        issue.id = root.get("idReadable").asString
        return issue
    }

}


