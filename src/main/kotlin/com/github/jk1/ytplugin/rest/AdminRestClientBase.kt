package com.github.jk1.ytplugin.rest


interface AdminRestClientBase{

    fun getVisibilityGroups(issueId: String): List<String>

    fun getAccessibleProjects(): List<String>
}