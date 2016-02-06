package com.github.jk1.ytplugin.rest

import com.intellij.openapi.project.Project


class AdminRestClient(override val project: Project) : AbstractRestClient(project) {

    fun getUserGroups(issueId : String): List<String> {
        return listOf("All Users")
    }
}