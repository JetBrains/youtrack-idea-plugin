package com.github.jk1.ytplugin.rest

import com.intellij.openapi.project.Project


class AdminRestClient(override val project: Project) : AbstractRestClient(project) {

    fun getUserGroups(login : String): List<String> {
        return listOf("All Users")
    }
}