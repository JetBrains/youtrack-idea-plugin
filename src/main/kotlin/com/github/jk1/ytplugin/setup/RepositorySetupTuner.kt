package com.github.jk1.ytplugin.setup

import com.github.jk1.ytplugin.logger
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.tasks.TaskManager
import com.intellij.tasks.config.RecentTaskRepositories
import com.intellij.tasks.impl.TaskManagerImpl
import com.intellij.tasks.youtrack.YouTrackRepository
import com.intellij.tasks.youtrack.YouTrackRepositoryType
import com.intellij.util.net.HttpConfigurable
import io.netty.handler.codec.http.HttpScheme
import java.awt.Color
import javax.swing.JLabel

class RepositorySetupTuner(private val connectedRepository: YouTrackRepository,
                           private val repoConnector: SetupRepositoryConnector,
                           private val url: String,
                           private val password: String) {


    companion object {
        fun tuneRepoToStoreBasedOnConnectedRepo(repositoryToStore: YouTrackRepository, connectedRepository: YouTrackRepository) {
            repositoryToStore.isLoginAnonymously = false

            repositoryToStore.url = connectedRepository.url
            repositoryToStore.password = connectedRepository.password
            repositoryToStore.username = connectedRepository.username
            repositoryToStore.repositoryType = connectedRepository.repositoryType
            repositoryToStore.storeCredentials()

            repositoryToStore.isShared = connectedRepository.isShared
            repositoryToStore.isUseProxy = connectedRepository.isUseProxy
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

    fun setupRepositoryParameters(isShared: Boolean) {
        val myRepositoryType = YouTrackRepositoryType()
        connectedRepository.isLoginAnonymously = false
        if (url.startsWith(HttpScheme.HTTP.toString())) {
            connectedRepository.url = url
        } else {
            connectedRepository.url = HttpScheme.HTTP.toString() + "://" + url
        }
        connectedRepository.password = password
        connectedRepository.username = "random" // ignored by YouTrack anyway when token is sent as password
        connectedRepository.repositoryType = myRepositoryType
        connectedRepository.storeCredentials()
        connectedRepository.isShared = isShared
    }


    fun setupProxy(project: Project, isUseProxySelected: Boolean) {
        val proxy = HttpConfigurable.getInstance()
        if (proxy.PROXY_HOST != null || !isUseProxySelected) {
            connectedRepository.isUseProxy = isUseProxySelected
            if (url.isNotEmpty() && password.isNotEmpty()) {
                repoConnector.testConnection(connectedRepository, project)
                connectedRepository.storeCredentials()
            }
        } else {
            repoConnector.noteState = NotifierState.NULL_PROXY_HOST
            connectedRepository.isUseProxy = false
        }
    }

    fun setNotifierState(credentialsChecker: CredentialsChecker) {
        if (url.isEmpty() || password.isEmpty()) {
            repoConnector.noteState = NotifierState.EMPTY_FIELD
        } else if (!(credentialsChecker.isMatchingAppPassword(connectedRepository.password)
                    || credentialsChecker.isMatchingBearerToken(connectedRepository.password))
        ) {
            repoConnector.noteState = NotifierState.INVALID_TOKEN
        } else if (PasswordSafe.instance.isMemoryOnly) {
            repoConnector.noteState = NotifierState.PASSWORD_NOT_STORED
        }
    }

    fun setNotifier(note: JLabel, noteState: NotifierState) {
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
    PASSWORD_NOT_STORED,
    INSUFFICIENT_FOR_TOKEN_VERSION
}