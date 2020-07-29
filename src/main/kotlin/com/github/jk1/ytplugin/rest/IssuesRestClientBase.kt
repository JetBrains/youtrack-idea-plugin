package com.github.jk1.ytplugin.rest

import com.github.jk1.ytplugin.issues.model.Issue
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.google.gson.JsonParser
import net.minidev.json.JSONObject
import org.apache.commons.httpclient.methods.GetMethod
import org.apache.commons.httpclient.methods.PostMethod
import org.apache.commons.httpclient.methods.StringRequestEntity
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

/**
 * Fetches YouTrack issues with issue description formatted from wiki into html on server side.
 */
interface IssuesRestClientBase {

    fun createDraft(summary: String): String

    fun getIssue(id: String): Issue?

    fun getIssues(query: String = ""): List<Issue>

    fun getWikifiedIssuesInProject(projectShortName: String, query: String = ""): List<Issue>

}