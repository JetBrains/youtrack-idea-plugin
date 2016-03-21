package com.github.jk1.ytplugin.navigator.components

import com.github.jk1.ytplugin.common.components.ComponentAware
import com.github.jk1.ytplugin.common.logger
import com.github.jk1.ytplugin.navigator.ConnectionHandler
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
    }

    override fun projectClosed() {
        try {
            httpServer?.stop()
        } catch(e: Exception) {
            logger.warn("Failed to stop embedded http server for project ${project.name}", e)
        }
    }
}