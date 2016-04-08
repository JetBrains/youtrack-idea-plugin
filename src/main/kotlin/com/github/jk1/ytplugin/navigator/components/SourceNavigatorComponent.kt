package com.github.jk1.ytplugin.navigator.components

import com.github.jk1.ytplugin.common.components.ComponentAware
import com.github.jk1.ytplugin.common.logger
import com.github.jk1.ytplugin.common.sendNotification
import com.github.jk1.ytplugin.navigator.ConnectionHandler
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.AbstractProjectComponent
import com.intellij.openapi.project.Project
import fi.iki.elonen.NanoHTTPD
import java.io.IOException
import java.net.ServerSocket

/**
 * Embedded HTTP server component to support 'Open in IDE' feature.
 * For any stack trace submitted to YouTrack user can click on a stack trace element to get the corresponding
 * line opened in IDE. This component listens for YouTrack requests to open the code in question.
 *
 * TeamCity plugin listens on the same ports as well and both plugins can coexists within the port range.
 */
class SourceNavigatorComponent(override val project: Project) : AbstractProjectComponent(project), ComponentAware {

    private val eligiblePorts = 63330..63339
    private var httpServer: NanoHTTPD? = null

    override fun projectOpened() {
        try {
            val port = eligiblePorts.firstOrNull {
                try {
                    ServerSocket(it).close()
                    true
                } catch(e: IOException) {
                    logger.debug("Can't use port $it to listen for YouTrack connections: ${e.message}")
                    false
                }
            } ?: throw IllegalStateException("Can't listen on ports $eligiblePorts. Is there a firewall enabled?")

            httpServer = ConnectionHandler(project, port)
            httpServer?.start()
        } catch(e: Exception) {
            logger.debug(e)
            sendNotification(
                    text = "Can't listen on ports 63330 to 63339. 'Open in IDE' feature will be disabled",
                    type = NotificationType.WARNING)
        }
    }

    override fun projectClosed() {
        try {
            httpServer?.stop()
        } catch(e: Exception) {
            logger.warn("Failed to stop embedded http server for project ${project.name}", e)
        }
    }
}