// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.github.jk1.ytplugin.setupWindow
import com.github.jk1.ytplugin.logger
import com.github.jk1.ytplugin.setupWindow.Connection.CancellableConnection
import com.github.jk1.ytplugin.setupWindow.Connection.HttpTestConnection
import com.github.jk1.ytplugin.setupWindow.Connection.TestConnectionTask
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.tasks.youtrack.YouTrackRepository
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.io.HttpRequests
import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.Tag
import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.NameValuePair
import org.apache.commons.httpclient.URI
import org.apache.commons.httpclient.UsernamePasswordCredentials
import org.apache.commons.httpclient.auth.AuthScope
import org.apache.commons.httpclient.methods.GetMethod
import org.apache.commons.httpclient.methods.PostMethod
import org.jetbrains.annotations.TestOnly
import java.awt.Color
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import javax.swing.JLabel

/**
 * Login errors classification
 */
enum class NotifierState{
    SUCCESS, LOGIN_ERROR, UNKNOWN_ERROR, UNKNOWN_HOST, INVALID_TOKEN, INVALID_VERSION
}

/**
 * Class for the task management
 */
class SetupManager() {

    var correctUrl: String = ""
    var noteState = NotifierState.INVALID_TOKEN

    @TestOnly
    var statusCode = 401

    @Tag("username")
    fun getRepositoryUsername(repository: YouTrackRepository): String? {
        return repository.username
    }

    @Tag("password")
    fun getRepositoryPassword(repository: YouTrackRepository): String? {
        return repository.password
    }

    fun trimTrailingSlashes(url: String?): String? {
        if (url == null) return ""
        for (i in url.length - 1 downTo 0) {
            if (url[i] != '/') {
                return url.substring(0, i + 1)
            }
        }
        return ""
    }

    @Attribute("url")
    fun getRepositoryUrl(repository: YouTrackRepository): String? {
        return trimTrailingSlashes(repository.url)
    }

    private fun isValidToken(token: String): Boolean {
        val tokenPattern = Regex("perm:([^.]+)\\.([^.]+)\\.(.+)")
        return token.matches(tokenPattern)
    }

    fun setNotifier(note: JLabel) {
        note.foreground = Color.red
        when (noteState) {
            NotifierState.SUCCESS -> {
                note.foreground = Color.green;
                note.text = "Successfully connected"
            }
            NotifierState.LOGIN_ERROR -> note.text = "Cannot login: incorrect URL or token"
            NotifierState.UNKNOWN_ERROR -> note.text = "Unknown error"
            NotifierState.UNKNOWN_HOST -> note.text = "Unknown host"
            NotifierState.INVALID_TOKEN -> note.text = "Invalid token"
            NotifierState.INVALID_VERSION -> note.text = "<html>Incompatible YouTrack version,<br/>please update to 2017.1</html>"
        }
    }

    private fun fixURI(method: PostMethod) {
        if (!method.uri.toString().contains("https")) {
            val newUri = "https" + method.uri.toString().substring(4, method.uri.toString().length)
            method.uri = URI(newUri, false)
        }
        if (!method.uri.toString().contains("/youtrack")) {
            val newUri = method.uri.toString() + "/youtrack"
            method.uri = URI(newUri, false)
        } else {
            throw HttpRequests.HttpStatusException("Cannot login: incorrect URL or token", method.statusCode, method.path)
        }
    }

    private fun fixURI(uri: String): String {
        var newUri = uri
        if (!uri.contains("https")) {
            newUri = "https" + uri.substring(4, uri.length)
        }
        if (!uri.contains("/youtrack")) {
            newUri = "$newUri/youtrack"
        }
        return newUri
    }

    @Throws(java.lang.Exception::class)
    private fun login(method: PostMethod, repository: YouTrackRepository): HttpClient? {
        val client: HttpClient = repository.httpClient
        client.state.setCredentials(AuthScope.ANY, UsernamePasswordCredentials(getRepositoryUsername(repository), getRepositoryPassword(repository)))
        method.addParameter("login", getRepositoryUsername(repository))
        method.addParameter("password", getRepositoryPassword(repository))
        client.params.contentCharset = "UTF-8"
        client.executeMethod(method)

        val response: String?
        response = try {
            statusCode = method.statusCode
            when {
                statusCode in 301..399 -> {
                    val location: String = method.getResponseHeader("Location").toString()
                    val newUri = location.substring(10, location.length)
                    method.uri = URI(newUri, false)
                    repository.url = method.uri.toString()
                    correctUrl = repository.url
                    createCancellableConnection(repository)
                }
                statusCode == 403 -> {
                    throw java.lang.Exception("Cannot login: token error occurred or user was banned")
                }
                statusCode != 200 -> {
                    fixURI(method)
                    /* substring(0,  newUri.length - 12) is needed to get rid of "/api/token" ending */
                    repository.url = method.uri.toString().substring(0, method.uri.toString().length - 12)
                    createCancellableConnection(repository)
                }
            }
            method.getResponseBodyAsString(1000)
        } finally {
            method.releaseConnection()
        }
        if (response == null) {
            throw NullPointerException()
        }
        return client
    }


    fun createCancellableConnection(repository: YouTrackRepository): CancellableConnection? {
        val newUri = fixURI(repository.url)
        repository.url = newUri
        correctUrl = repository.url

        val method = PostMethod(getRepositoryUrl(repository) + "/api/token")
        method.setRequestHeader("Authorization", "Bearer " + repository.password)

        return object : HttpTestConnection<PostMethod?>(method) {
            @Throws(java.lang.Exception::class)
            override fun doTest(method: PostMethod?) {
                if (method != null) {
                    login(method, repository)
                }
            }
        }
    }


    fun testConnection(repository: YouTrackRepository, myProject: Project): Boolean {

        if (!repository.isLoginAnonymously && !isValidToken(repository.password)) {
            noteState = NotifierState.INVALID_TOKEN
            return false
        }

        val myBadRepositories = ContainerUtil.newConcurrentSet<YouTrackRepository>()

        val task: TestConnectionTask = object : TestConnectionTask("Test connection", myProject) {
            override fun run(indicator: ProgressIndicator) {
                indicator.text = "Connecting to " + repository.url + "..."
                indicator.fraction = 0.0
                indicator.isIndeterminate = true
                correctUrl = repository.url
                try {
                    myConnection = createCancellableConnection(repository)
                    if (myConnection != null) {
                        val future = ApplicationManager.getApplication().executeOnPooledThread(myConnection!!)
                        while (true) {
                            try {
                                myException = future[100, TimeUnit.MILLISECONDS]
                                noteState = NotifierState.LOGIN_ERROR
                                return
                            } catch (ignore: TimeoutException) {
                                try {
                                    indicator.checkCanceled()
                                } catch (e: ProcessCanceledException) {
                                    myException = e
                                    noteState = NotifierState.LOGIN_ERROR
                                    myConnection!!.cancel()
                                    return
                                }
                            } catch (e: Exception) {
                                myException = e
                                noteState = NotifierState.LOGIN_ERROR
                                return
                            }
                        }
                    }
                } catch (e: Exception) {
                    myException = e
                }
            }
        }

        ProgressManager.getInstance().run(task)
        val e = task.myException
        if (e == null) {
            myBadRepositories.remove(repository)
            correctUrl = repository.url
            if (isValidVersion(repository)) {
                noteState = NotifierState.SUCCESS
            }
            else
                noteState = NotifierState.INVALID_VERSION
        } else if (e !is ProcessCanceledException) {
            val message = e.message
            if (e is UnknownHostException) {
                noteState = NotifierState.UNKNOWN_HOST
            }
            if (message == null) {
                noteState = NotifierState.UNKNOWN_ERROR
            }
        }
        return e == null
    }

    private fun isValidVersion(repository: YouTrackRepository): Boolean {
        val client = HttpClient()
        val method = GetMethod(getRepositoryUrl(repository) + "/api/config")
        val fields = NameValuePair("fields", "version")
        method.setQueryString(arrayOf(fields))
        method.setRequestHeader("Authorization", "Bearer " + repository.password)
        val status = client.executeMethod(method)

        return if (status == 200) {
            val json: JsonObject = JsonParser.parseString(method.responseBodyAsString) as JsonObject
            if (json.get("version") == null || json.get("version").isJsonNull) {
                noteState = NotifierState.LOGIN_ERROR
                false
            } else {
                (json.get("version").asString.toFloat() >= 2017.1)
            }
        }
        else {
            noteState = NotifierState.LOGIN_ERROR
            logger.warn("INVALID LOGIN OR TOKEN, FAILED ON VERSION VALIDATION: ${method.statusCode}")
            false
        }
    }
}


