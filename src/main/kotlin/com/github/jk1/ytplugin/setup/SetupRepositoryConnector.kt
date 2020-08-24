package com.github.jk1.ytplugin.setup

import com.github.jk1.ytplugin.logger
import com.github.jk1.ytplugin.setup.connection.CancellableConnection
import com.github.jk1.ytplugin.setup.connection.HttpTestConnection
import com.github.jk1.ytplugin.setup.connection.TestConnectionTask
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.tasks.TaskManager
import com.intellij.tasks.TaskRepository
import com.intellij.tasks.config.RecentTaskRepositories
import com.intellij.tasks.impl.TaskManagerImpl
import com.intellij.tasks.youtrack.YouTrackRepository
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.io.HttpRequests
import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.NameValuePair
import org.apache.commons.httpclient.URI
import org.apache.commons.httpclient.UsernamePasswordCredentials
import org.apache.commons.httpclient.auth.AuthScope
import org.apache.commons.httpclient.methods.GetMethod
import org.apache.commons.httpclient.methods.PostMethod
import java.awt.Color
import java.io.InputStreamReader
import java.net.UnknownHostException
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import javax.swing.JLabel

class SetupRepositoryConnector {

    var correctUrl: String = ""
    var noteState = NotifierState.INVALID_TOKEN
    var isEndingFixed = false
    var statusCode = 401

    fun getRepositoryUsername(repository: YouTrackRepository): String? {
        return repository.username
    }

    fun getRepositoryPassword(repository: YouTrackRepository): String? {
        return repository.password
    }

    fun getRepositoryUrl(repository: YouTrackRepository): String? {
        return trimTrailingSlashes(repository.url)
    }


    private fun trimTrailingSlashes(url: String?): String? {
        if (url == null) return ""
        for (i in url.length - 1 downTo 0) {
            if (url[i] != '/') {
                return url.substring(0, i + 1)
            }
        }
        return ""
    }

    private fun isValidToken(token: String): Boolean {
        val tokenPattern = Regex("perm:([^.]+)\\.([^.]+)\\.(.+)")
        return token.matches(tokenPattern)
    }

    fun setNotifier(note: JLabel) {
        note.foreground = Color.red
        when (noteState) {
            NotifierState.SUCCESS -> {
                note.foreground = Color.green
                note.text = "Successfully connected"
            }
            NotifierState.LOGIN_ERROR -> note.text = "Cannot login: incorrect URL or token"
            NotifierState.UNKNOWN_ERROR -> note.text = "Unknown error"
            NotifierState.UNKNOWN_HOST -> note.text = "Unknown host"
            NotifierState.INVALID_TOKEN -> note.text = "Invalid token"
            NotifierState.NULL_PROXY_HOST -> note.text = "Invalid proxy host"
            NotifierState.INVALID_VERSION -> note.text = "<html>Incompatible YouTrack version,<br/>please update to 2017.1</html>"
        }
    }


    private fun fixEnding(uri: String): String {
        var newUri = uri
        if (!uri.contains("/youtrack")) {
            newUri = "$newUri/youtrack"
        }
        isEndingFixed = true
        return newUri
    }

    private fun fixProtocol(oldUrl: URI): URI {
        val url = URI("https",null, oldUrl.host, oldUrl.port, oldUrl.path, oldUrl.query, oldUrl.fragment)

        return url
    }

    private fun isValidYouTrackVersion(repository: YouTrackRepository): Boolean {
        val client = HttpClient()
        val method = GetMethod(getRepositoryUrl(repository) + "/api/config")
        val fields = NameValuePair("fields", "version")
        method.setQueryString(arrayOf(fields))
        method.setRequestHeader("Authorization", "Bearer " + repository.password)
        val status = client.executeMethod(method)

        return if (status == 200) {
            val reader = InputStreamReader(method.responseBodyAsStream, StandardCharsets.UTF_8)
            val json: JsonObject = JsonParser.parseReader(reader).asJsonObject
            if (json.get("version") == null || json.get("version").isJsonNull) {
                noteState = NotifierState.LOGIN_ERROR
                false
            } else {
                (json.get("version").asString.toDouble() >= 2017.1)
            }
        }
        else {
            noteState = NotifierState.LOGIN_ERROR
            logger.warn("INVALID LOGIN OR TOKEN, FAILED ON VERSION VALIDATION: ${method.statusCode}")
            false
        }
    }


    @Throws(java.lang.Exception::class)
    private fun login(method: PostMethod, repository: YouTrackRepository): HttpClient? {
        val client = repository.httpClient
        val credentials = UsernamePasswordCredentials(getRepositoryUsername(repository), getRepositoryPassword(repository))
        client.state.setCredentials(AuthScope.ANY, credentials)
        method.addParameter("login", getRepositoryUsername(repository))
        method.addParameter("password", getRepositoryPassword(repository))
        client.params.contentCharset = "UTF-8"
        client.executeMethod(method)

        try {
            statusCode = method.statusCode
            when {
                statusCode in 301..399 -> {
                    if (isEndingFixed || repository.url.contains("/youtrack")){
                        val tmp = method.uri
                        val new = URI(tmp.toString().substring(0, tmp.toString().length - 10), false)
                        val newUri = fixProtocol(new)
                        method.uri = URI("https://alinaboshchenko.myjetbrains.com/youtrack", false)
                        repository.url = newUri.toString()
                        correctUrl = repository.url
                    }
                    else{
                        val newUri = fixEnding(repository.url)
                        method.uri = URI(newUri, false)
                        repository.url = newUri
                        correctUrl = repository.url
                    }

                    createCancellableConnection(repository)
                }
                statusCode == 403 -> {
                    throw java.lang.Exception("Cannot login: token error occurred or user was banned")
                }
                statusCode != 200 -> {
                    println("hrr")
                    noteState == NotifierState.LOGIN_ERROR
                    throw HttpRequests.HttpStatusException("Cannot login: incorrect URL or token", method.statusCode, method.path)
                }
            }
            method.getResponseBodyAsString(1000) ?: throw IllegalStateException("No response received")
        } finally {
            method.releaseConnection()
        }
        return client
    }


    fun createCancellableConnection(repository: YouTrackRepository): CancellableConnection? {
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
            if (isValidYouTrackVersion(repository)) {
                noteState = NotifierState.SUCCESS
            }
            else
                if (noteState != NotifierState.LOGIN_ERROR)
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

    fun showIssuesForConnectedRepo(repository: YouTrackRepository, project: Project) {
        val myManager: TaskManagerImpl = TaskManager.getManager(project) as TaskManagerImpl
        lateinit var myRepositories: List<YouTrackRepository>
        myRepositories = arrayListOf(repository)
        val newRepositories: List<TaskRepository> = ContainerUtil.map<TaskRepository, TaskRepository>(myRepositories) { obj: TaskRepository -> obj.clone() }
        myManager.setRepositories(newRepositories)
        myManager.updateIssues(null)
        RecentTaskRepositories.getInstance().addRepositories(myRepositories)
    }
}

/**
 * Login errors classification
 */
enum class NotifierState{
    SUCCESS, LOGIN_ERROR, UNKNOWN_ERROR, UNKNOWN_HOST, INVALID_TOKEN, INVALID_VERSION, NULL_PROXY_HOST
}

