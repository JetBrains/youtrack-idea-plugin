package com.github.jk1.ytplugin.rest

// todo: convert me to use json api
interface AdminRestClientBase{

    fun getVisibilityGroups(issueId: String): List<String>

    fun getAccessibleProjects(): List<String>
}