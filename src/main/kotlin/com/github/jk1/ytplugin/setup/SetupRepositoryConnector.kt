package com.github.jk1.ytplugin.setup

import com.github.jk1.ytplugin.logger
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.tasks.TaskManager
import com.intellij.tasks.TaskRepository
import com.intellij.tasks.config.RecentTaskRepositories
import com.intellij.tasks.impl.TaskManagerImpl
import com.intellij.tasks.youtrack.YouTrackRepository
import com.intellij.util.containers.ContainerUtil
import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.HttpMethod
import org.apache.commons.httpclient.NameValuePair
import org.apache.commons.httpclient.URI
import org.apache.commons.httpclient.methods.GetMethod
import java.awt.Color
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import javax.net.ssl.SSLException
import javax.swing.JLabel

class SetupRepositoryConnector {
    var noteState = NotifierState.INVALID_TOKEN
    fun isValidToken(token: String): Boolean {
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
            NotifierState.GUEST_LOGIN -> {
                note.foreground = Color.green
                note.text = "Logged in as a guest"
            }
            NotifierState.EMPTY_FIELD -> note.text =  "Url and token fields are mandatory"
            NotifierState.LOGIN_ERROR -> note.text = "Cannot login: incorrect URL or token"
            NotifierState.UNKNOWN_ERROR -> note.text = "Unknown error"
            NotifierState.UNKNOWN_HOST -> note.text = "Unknown host"
            NotifierState.INVALID_TOKEN -> note.text = "Invalid token"
            NotifierState.NULL_PROXY_HOST -> note.text = "Invalid proxy host"
            NotifierState.INCORRECT_CERTIFICATE -> note.text = "Incorrect certificate"
            NotifierState.TIMEOUT -> note.text = "Connection timeout: check login and token"
            NotifierState.UNAUTHORIZED -> note.text = "Unauthorized: check login and token"
            NotifierState.INVALID_VERSION -> note.text = "<html>Incompatible YouTrack version,<br/>please update to 2017.1</html>"
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

    private fun checkAndFixConnection(repository: YouTrackRepository): Future<*> {
        val future = CompletableFuture<Any>()
        val checker = ConnectionChecker(repository)

        checker.onSuccess {
            noteState = if (isValidYouTrackVersion(repository)) {
                logger.debug("valid YouTrack version detected")
                NotifierState.SUCCESS
            } else {
                logger.debug("invalid YouTrack version detected")
                NotifierState.INVALID_VERSION
            }
            future.complete(Unit)
        }
        checker.onApplicationError { method, code ->
            logger.debug("handling application error for ${repository.url}")
            when (code) {
                in 301..399 -> {
                    logger.debug("handling response code 301..399 for the ${repository.url}: REDIRECT")
                    val location: String = method.getResponseHeader("Location").toString()
                    // received: "Location: {new_repository.url}/api/token", thus needs to be cut
                    repository.url = location.substring(10, location.length - 11)
                    logger.debug("url after correction: ${repository.url}")
                    checker.check()
                }
                403 -> {
                    logger.debug("handling response code 403 for the ${repository.url}: UNAUTHORIZED")
                    noteState = NotifierState.UNAUTHORIZED
                    future.complete(Unit)
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
                        future.complete(Unit)
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
                    future.complete(Unit)
                }
                else -> {
                    if (method.uri.scheme != "https") {
                        logger.debug("handling transport error for ${repository.url}: MANUAL PROTOCOL FIX")
                        val repoUri = URI(repository.url, false)
                        repository.url = if (repoUri.port != -1){
                            "https://" + repoUri.host + repoUri.port + repoUri.path
                        } else {
                            "https://" + repoUri.host  + repoUri.path
                        }
                        logger.debug("url after manual protocol fix: ${repository.url}")
                        checker.check()
                    } else {
                        logger.debug("no manual transport fix: LOGIN_ERROR")
                        noteState = NotifierState.LOGIN_ERROR
                        future.complete(Unit)
                    }
                }
            }
        }
        checker.check()
        return future
    }


    fun testConnection(repository: YouTrackRepository, myProject: Project) {
        logger.debug("TRY CONNECTION FOR ${repository.url}")

        val task = object : Task.Modal(myProject, "Test connection", true) {
            override fun run(indicator: ProgressIndicator) {
                indicator.text = "Connecting to " + repository.url + "..."
                indicator.fraction = 0.0
                indicator.isIndeterminate = true

                val future = checkAndFixConnection(repository)

                try {
                    future.get(15, TimeUnit.SECONDS)
                } catch (ignore: TimeoutException) {
                    logger.debug("could not connect to ${repository.url}: CONNECTION TIMEOUT")
                    noteState = NotifierState.TIMEOUT
                    indicator.stop()
                }
            }
        }
        ProgressManager.getInstance().run(task)
    }

    fun showIssuesForConnectedRepo(repository: YouTrackRepository, project: Project) {
        logger.debug("showing issues for ${repository.url}...")
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
    GUEST_LOGIN
}