package com.github.jk1.ytplugin.navigator

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.logger
import com.github.jk1.ytplugin.notifications.IdeNotificationsTrait
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import fi.iki.elonen.NanoHTTPD
import java.io.IOException

/**
 * Embedded HTTP server component to support 'Open in IDE' feature.
 * For any stack trace submitted to YouTrack user can click on a stack trace element to get the corresponding
 * line opened in IDE. This component listens for YouTrack requests to open the code in question.
 *
 * TeamCity plugin listens on the same ports as well and both plugins can coexists within the port range.
 */
@Service
class SourceNavigatorService(override val project: Project): ComponentAware, Disposable, IdeNotificationsTrait {

    private val eligiblePorts = 63330..63339
    private var httpServer: NanoHTTPD? = null

    init {
        val port = eligiblePorts.firstOrNull {
            try {
                val server = ConnectionHandler(project, it)
                server.start()
                httpServer = server
                true
            } catch (e: IOException) {
                logger.debug("Can't use port $it to listen for YouTrack connections: ${e.message}")
                false
            }
        }
        if (port == null) {
            showWarning(text = "Can't listen on ports 63330 to 63339. 'Open in IDE' feature will be disabled")
        }
    }

    override fun dispose() {
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                httpServer?.stop()
            } catch (e: Exception) {
                logger.warn("Failed to stop embedded http server for project ${project.name}", e)
            }
        }
    }

    fun getActivePort() = httpServer?.listeningPort
}