package com.github.jk1.ytplugin.navigator

import com.github.jk1.ytplugin.components.ComponentAware
import com.github.jk1.ytplugin.logger
import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.fileEditor.ex.FileEditorProviderManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import fi.iki.elonen.NanoHTTPD
import java.io.IOException
import java.net.ServerSocket


class SourceNavigatorServer(override val project: Project) : ComponentAware {

    private val eligiblePorts = 63330..63339
    private var httpServer: NanoHTTPD? = null

    fun start() {
        httpServer = ConnectionHandler(eligiblePorts.firstOrNull {
            try {
                ServerSocket(it).close()
                true
            } catch(e: IOException) {
                logger.debug("Can't use port $it to listen for YouTrack connections: ${e.message}")
                false
            }
        } ?: throw IllegalStateException("Can't listen on ports $eligiblePorts"))
        httpServer?.start()
    }

    fun stop() = httpServer?.stop()

    inner class ConnectionHandler(port: Int) : NanoHTTPD("127.0.0.1", port) {
        override fun serve(session: IHTTPSession): Response {
            if ("/file".equals(session.uri)) {
                return activateFile(session.parms)
            }
            val response = NanoHTTPD.newFixedLengthResponse(Response.Status.NOT_IMPLEMENTED, null, null, 0)
            response.closeConnection(true)
            return response
        }

        private fun activateFile(params: Map<String, String>): Response {
            val application = ApplicationManager.getApplication()
            var succeed = false
            application.invokeLater {
                application.runReadAction {
                    if (project.isInitialized) {
                        // todo: vcs lookup
                        // todo: params validation
                        succeed = tryOpenFileInProject(project, params["file"]!!, params)
                    }

                }
            }
            // todo: report errors
            val response = NanoHTTPD.newFixedLengthResponse(Response.Status.OK, null, null, 0)
            response.closeConnection(true)
            return response
        }

        private fun tryOpenFileInProject(project: Project, relativeFilePath: String, params: Map<String, String>): Boolean {
            val file = VirtualFileFinder.findFile(relativeFilePath, project)
            if (file == null) {
                return false
            } else {
                return navigateTo(project, file, params)
            }
        }

        private fun navigateTo(project: Project, virtualFile: VirtualFile, params: Map<String, String>): Boolean {
            val editorProviderManager = FileEditorProviderManager.getInstance()
            if (editorProviderManager.getProviders(project, virtualFile).size == 0) {
                return false
            } else {
                val descriptor = OpenFileDescriptor(project, virtualFile)
                val editor = FileEditorManager.getInstance(project).openTextEditor(descriptor, true)
                if (editor != null) {
                    val caretModel = editor.caretModel
                    if (params.containsKey("line")) {
                        try {
                            val ignore = LogicalPosition(Integer.valueOf(params["line"]).toInt() - 1, 0)
                            caretModel.moveToLogicalPosition(ignore)
                        } catch (e: NumberFormatException) {
                        }

                    }

                    if (params.containsKey("offset")) {
                        try {
                            caretModel.moveToOffset(Integer.parseInt(params["offset"]))
                        } catch (e: NumberFormatException) {
                        }

                    }

                    ApplicationManager.getApplication().invokeLater { editor.scrollingModel.scrollToCaret(ScrollType.CENTER) }
                }

                ProjectUtil.focusProjectWindow(project, true);
                return true
            }
        }
    }
}