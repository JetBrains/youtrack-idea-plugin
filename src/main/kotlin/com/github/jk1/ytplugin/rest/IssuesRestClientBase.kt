package com.github.jk1.ytplugin.rest

import com.github.jk1.ytplugin.issues.model.Issue

/**
 * Fetches YouTrack issues with issue description formatted from wiki into html on server side.
 */
interface IssuesRestClientBase {

    fun createDraft(summary: String): String

    fun getIssue(id: String): Issue?

    fun getIssues(query: String = ""): List<Issue>

}