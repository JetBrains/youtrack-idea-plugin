package com.github.jk1.ytplugin.setup

import com.github.jk1.ytplugin.logger
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.tasks.TaskManager
import com.intellij.tasks.config.RecentTaskRepositories
import com.intellij.tasks.impl.TaskManagerImpl
import com.intellij.tasks.youtrack.YouTrackRepository
import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.HttpMethod
import org.apache.commons.httpclient.NameValuePair
import org.apache.commons.httpclient.URI
import org.apache.commons.httpclient.methods.GetMethod
import java.awt.Color
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import javax.net.ssl.SSLException
import javax.swing.JLabel

class SetupRepositoryConnector {

    @Volatile
    var noteState = NotifierState.INVALID_TOKEN
    private val tokenPattern = Regex("perm:([^.]+)\\.([^.]+)\\.(.+)")

    fun isValidToken(token: String): Boolean {
        return token.matches(tokenPattern)
    }

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
            NotifierState.EMPTY_FIELD -> note.text =  "Url and token fields are mandatory"
            NotifierState.PASSWORD_NOT_STORED -> note.text =  "Please check if password persistence is permitted in System Settings "
            NotifierState.LOGIN_ERROR -> note.text = "Cannot login: incorrect URL or token"
            NotifierState.UNKNOWN_ERROR -> note.text = "Unknown error"
            NotifierState.UNKNOWN_HOST -> note.text = "Unknown host"
            NotifierState.INVALID_TOKEN -> note.text = "Invalid token"
            NotifierState.NULL_PROXY_HOST -> note.text = "Invalid proxy host"
            NotifierState.INCORRECT_CERTIFICATE -> note.text = "Incorrect certificate"
            NotifierState.TIMEOUT -> note.text = "Connection timeout: check login and token"
            NotifierState.UNAUTHORIZED -> note.text = "Unauthorized: check login and token"
            NotifierState.INVALID_VERSION -> note.text = "<html>Incompatible YouTrack version,<br/>please update to 2017.1 or later</html>"
        }
    }

    private fun isValidYouTrackVersion(repository: YouTrackRepository): Boolean {
        val client = HttpClient()
        val method = GetMethod(repository.url.trimEnd('/') + "/api/config")
        val fields = NameValuePair("fields", "version")
        method.setQueryString(arrayOf(fields))
        method.setRequestHeader("Authorization", "Bearer " + repository.password)

        try {
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
            } else {
                noteState = NotifierState.LOGIN_ERROR
                logger.warn("invalid token or login, failed on version validation: ${method.statusCode}")
                false
            }
        } catch (e: Exception) {
            logger.warn("invalid token or login, failed on version validation: ${method.statusCode}")
        }
        return false
    }

    private fun checkAndFixConnection(repository: YouTrackRepository) {
        val checker = ConnectionChecker(repository)
        checker.onSuccess { method ->
            noteState = if (isValidYouTrackVersion(repository)) {
                repository.url = method.uri.toString().replace("/api/token", "")
                logger.debug("valid YouTrack version detected")
                NotifierState.SUCCESS
            } else {
                logger.debug("invalid YouTrack version detected")
                NotifierState.INVALID_VERSION
            }
        }
        checker.onApplicationError { method, code ->
            logger.debug("handling application error for ${repository.url}")
            when (code) {
                in 301..399 -> {
                    logger.debug("handling response code 301..399 for the ${repository.url}: REDIRECT")
                    val location = method.getResponseHeader("Location").value
                    repository.url = location.replace("/api/token", "")
                    logger.debug("url after correction: ${repository.url}")
                    // unloaded instance redirect can't handle /api/* suffix properly
                    checker.check(api = !location.contains("/waitInstanceStartup/"))
                }
                401, 403 -> {
                    logger.debug("handling response code 403 for the ${repository.url}: UNAUTHORIZED")
                    noteState = NotifierState.UNAUTHORIZED
                }
                else -> {
                    logger.debug("handling response code other than 301..399, 403 ${repository.url}: MANUAL FIX")
                    if (!method.uri.path.contains("/youtrack")){
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
        checker.onTransportError { method: HttpMethod, exception: Exception ->
            logger.debug("handling transport error for ${repository.url}")
            when (exception) {
                is SSLException -> {
                    logger.debug("application error: INCORRECT_CERTIFICATE")
                    noteState = NotifierState.INCORRECT_CERTIFICATE
                }
                else -> {
                    if (method.uri.scheme != "https") {
                        logger.debug("handling transport error for ${repository.url}: MANUAL PROTOCOL FIX")
                        val repoUri = URI(repository.url, false)
                        repository.url = URI("https", null, repoUri.host, repoUri.port, repoUri.path).toString()
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
                checkAndFixConnection(repository)
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
    NULL_PROXY_HOST,
    TIMEOUT,
    INCORRECT_CERTIFICATE,
    UNAUTHORIZED,
    EMPTY_FIELD,
    GUEST_LOGIN,
    PASSWORD_NOT_STORED
}