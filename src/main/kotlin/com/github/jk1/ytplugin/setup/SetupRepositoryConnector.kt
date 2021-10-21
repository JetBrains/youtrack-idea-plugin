package com.github.jk1.ytplugin.setup

import com.github.jk1.ytplugin.logger
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.tasks.TaskManager
import com.intellij.tasks.config.RecentTaskRepositories
import com.intellij.tasks.impl.TaskManagerImpl
import com.intellij.tasks.youtrack.YouTrackRepository
import com.intellij.util.net.ssl.CertificateManager
import org.apache.http.HttpRequest
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.utils.URIBuilder
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClientBuilder
import java.awt.Color
import java.io.InputStreamReader
import java.net.URL
import java.nio.charset.StandardCharsets
import javax.net.ssl.SSLException
import javax.swing.JLabel

class SetupRepositoryConnector {

    companion object {
        fun setupHttpClient(): CloseableHttpClient {
            val config = RequestConfig.custom().setConnectTimeout(60000).build()
            return HttpClientBuilder.create()
                .disableRedirectHandling()
                .setSSLContext(CertificateManager.getInstance().sslContext)
                .setDefaultRequestConfig(config).build()
        }
    }

    @Volatile
    var noteState = NotifierState.INVALID_TOKEN

    fun setNotifier(note: JLabel) {
        note.foreground = Color.red
        when (noteState) {
            NotifierState.SUCCESS -> {
                note.foreground = Color(54, 156, 54)
                note.text = "Connection successful"
            }
            NotifierState.GUEST_LOGIN -> {
                note.foreground = Color(54, 156, 54)
                note.text = "Logged in as a guest"
            }
            NotifierState.EMPTY_FIELD -> note.text = "Url and token fields are mandatory"
            NotifierState.PASSWORD_NOT_STORED -> note.text = "Please check if password persistence is permitted in System Settings "
            NotifierState.LOGIN_ERROR -> note.text = "Cannot login: incorrect URL or token"
            NotifierState.UNKNOWN_ERROR -> note.text = "Unknown error"
            NotifierState.UNKNOWN_HOST -> note.text = "Unknown host"
            NotifierState.INVALID_TOKEN -> note.text = "Invalid token"
            NotifierState.NULL_PROXY_HOST -> note.text = "Invalid proxy host"
            NotifierState.INCORRECT_CERTIFICATE -> note.text = "Incorrect certificate"
            NotifierState.TIMEOUT -> note.text = "Connection timeout: check login and token"
            NotifierState.UNAUTHORIZED -> note.text = "Unauthorized: check login and token"
            NotifierState.INVALID_VERSION -> note.text = "<html>Incompatible YouTrack version,<br/>please update to 2017.1 or later</html>"
            NotifierState.INSUFFICIENT_FOR_TOKEN_VERSION -> note.text = "<html>YouTrack version is not compatible with" +
                    " token authentication,<br/>please use [username]:[application password] format to login</html>"
        }
    }

    private fun getYouTrackVersion(url: String): Double? {
        val builder = URIBuilder(url.trimEnd('/') + "/api/config")
        builder.addParameter("fields", "version")
        val method = HttpGet(builder.build())
        val client = setupHttpClient()

        try {
            val response = client.execute(method)
            return if (response.statusLine.statusCode == 200) {
                val reader = InputStreamReader(response.entity.content, StandardCharsets.UTF_8)
                val json: JsonObject = JsonParser.parseReader(reader).asJsonObject
                if (json.get("version") == null || json.get("version").isJsonNull) {
                    noteState = NotifierState.LOGIN_ERROR
                    null
                } else {
                    val version = json.get("version").asString.toDouble()
                    logger.debug("YouTrack version: $version")
                    version
                }
            } else {
                noteState = NotifierState.LOGIN_ERROR
                logger.warn("invalid token or login, failed on version validation: ${response.statusLine.statusCode}")
                null
            }
        } catch (e: Exception) {
            logger.warn("invalid token or login, failed on version validation: ${e.message}")
        }
        return null
    }

    private fun isValidYouTrackVersion(repository: YouTrackRepository): Boolean {
        val version = getYouTrackVersion(repository.url)
        return version != null && version >= 2017.1
    }


    private fun checkAndFixConnection(repository: YouTrackRepository, project: Project) {
        val checker = ConnectionChecker(repository, project)
        checker.onSuccess { request ->
            if (isValidYouTrackVersion(repository)) {
                repository.url = request.requestLine.uri.replace("/api/users/me?fields=name", "")
                logger.debug("valid YouTrack version detected")
                noteState = NotifierState.SUCCESS
            } else {
                logger.debug("invalid YouTrack version detected")
                if (noteState != NotifierState.LOGIN_ERROR){
                    noteState = NotifierState.INVALID_VERSION
                }
            }
        }
        checker.onVersionError { _ ->
            val version = getYouTrackVersion(repository.url)
            if (version != null && version >= 2017.1 && version <= 2020.4) {
                logger.debug("valid YouTrack version detected but it is not sufficient for bearer token usage")
                noteState = NotifierState.INSUFFICIENT_FOR_TOKEN_VERSION
            } else {
                logger.debug("guest login is not allowed")
                if (noteState != NotifierState.LOGIN_ERROR){
                    noteState = NotifierState.INVALID_TOKEN
                }
            }
        }
        checker.onApplicationError { request, response ->
            logger.debug("handling application error for ${repository.url}")
            when (response.statusLine.statusCode) {
                in 301..399 -> {
                    logger.debug("handling response code 301..399 for the ${repository.url}: REDIRECT")
                    val location = response.getFirstHeader("Location").value
                    if (!location.contains("/waitInstanceStartup/")) {
                        val endpoint = "/api/users/me?fields=name"
                        repository.url =  if (location.contains(endpoint)){
                           location.replace("/api/users/me?fields=name", "")
                        } else {
                           "${repository.url}$location"
                        }
                    } else {
                        if (!request.requestLine.uri.contains("/youtrack")) {
                            logger.debug("url after manual ending fix for waitInstanceStartup : ${repository.url}")
                            repository.url = "${repository.url}/youtrack"
                        }
                    }
                    logger.debug("url after correction: ${repository.url}")
                    // unloaded instance redirect can't handle /api/* suffix properly
                    checker.check()
                }
                401, 403 -> {
                    logger.debug("handling response code 403 for the ${repository.url}: UNAUTHORIZED")
                    noteState = NotifierState.UNAUTHORIZED
                }
                else -> {
                    logger.debug("handling response code other than 301..399, 403 ${repository.url}: MANUAL FIX")
                    if (!request.requestLine.uri.contains("/youtrack")) {
                        repository.url = "${repository.url}/youtrack"
                        logger.debug("url after manual ending fix: ${repository.url}")
                        checker.check()
                    } else {
                        logger.debug("no manual ending fix: LOGIN_ERROR")
                        noteState = NotifierState.LOGIN_ERROR
                    }
                }
            }
        }
        checker.onTransportError { request: HttpRequest, exception: Exception ->
            logger.debug("handling transport error for ${repository.url}")
            when (exception) {
                is SSLException -> {
                    logger.debug("application error: INCORRECT_CERTIFICATE")
                    noteState = NotifierState.INCORRECT_CERTIFICATE
                }
                else -> {
                    if (URL(request.requestLine.uri).protocol != "https") {
                        logger.debug("handling transport error for ${repository.url}: MANUAL PROTOCOL FIX")
                        val repoUrl = URL(repository.url)
                        repository.url = URL("https", repoUrl.host, repoUrl.port, repoUrl.path).toString()
                        logger.debug("url after manual protocol fix: ${repository.url}")
                        checker.check()
                    } else {
                        logger.debug("no manual transport fix: LOGIN_ERROR")
                        noteState = NotifierState.LOGIN_ERROR
                    }
                }
            }
        }
        checker.check()
    }

    fun testConnection(repository: YouTrackRepository, myProject: Project) {
        logger.debug("TRY CONNECTION FOR ${repository.url}")
        val task = object : Task.Modal(myProject, "Test connection", true) {
            override fun run(indicator: ProgressIndicator) {
                indicator.text = "Connecting to " + repository.url + "..."
                indicator.fraction = 0.0
                indicator.isIndeterminate = true

                checkAndFixConnection(repository, myProject)
            }
        }
        ProgressManager.getInstance().run(task)
    }

    fun showIssuesForConnectedRepo(repository: YouTrackRepository, project: Project) {
        logger.debug("showing issues for ${repository.url}...")
        val taskManager = TaskManager.getManager(project) as TaskManagerImpl
        taskManager.setRepositories(listOf(repository))
        taskManager.updateIssues(null)
        RecentTaskRepositories.getInstance().addRepositories(listOf(repository))
    }

    fun updateToolWindowName(project: Project, url: String) {
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("YouTrack")!!
        val content = toolWindow.contentManager.getContent(0)
        content?.displayName = ("${url.split("//").last()}   |   Issues")
    }
}

/**
 * Login errors classification
 */
enum class NotifierState {
    SUCCESS,
    LOGIN_ERROR,
    UNKNOWN_ERROR,
    UNKNOWN_HOST,
    INVALID_TOKEN,
    INVALID_VERSION,
    INVALID_PROTOCOL,
    NULL_PROXY_HOST,
    TIMEOUT,
    INCORRECT_CERTIFICATE,
    UNAUTHORIZED,
    EMPTY_FIELD,
    GUEST_LOGIN,
    PASSWORD_NOT_STORED,
    INSUFFICIENT_FOR_TOKEN_VERSION
}