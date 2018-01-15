package com.github.jk1.ytplugin.navigator

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


class OpenFileResource(val project: Project) : ConnectionHandler.Resource {

    override fun canHandle(session: NanoHTTPD.IHTTPSession) = "/file" == session.uri

    override fun handle(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        var response: NanoHTTPD.Response? = null
        val data = OpenFileData(session.parms)
        if (data.file != null) {
            readAction {
                val virtualFile = VirtualFileFinder.findFile(data.file, project)
                if (virtualFile != null) {
                    if (project.isInitialized && navigateTo(virtualFile, data)) {
                        response = successResponse()
                    }
                }
            }
        }
        return response ?: errorResponse()
    }

    private fun readAction(callback: () -> Unit) {
        val application = ApplicationManager.getApplication()
        application.invokeAndWait({
            application.runReadAction {
                callback.invoke()
            }
        }, application.noneModalityState)
    }

    private fun navigateTo(virtualFile: VirtualFile, data: OpenFileData): Boolean {
        val editorProviderManager = FileEditorProviderManager.getInstance()
        if (editorProviderManager.getProviders(project, virtualFile).isEmpty()) {
            return false
        } else {
            val descriptor = OpenFileDescriptor(project, virtualFile)
            val editor = FileEditorManager.getInstance(project).openTextEditor(descriptor, true)
            if (editor != null) {
                val caretModel = editor.caretModel
                if (data.line != null) {
                    caretModel.moveToLogicalPosition(LogicalPosition(data.line - 1, data.offset ?: 0))
                    ApplicationManager.getApplication().invokeLater {
                        editor.scrollingModel.scrollToCaret(ScrollType.CENTER)
                    }
                }
            }
            ProjectUtil.focusProjectWindow(project, true)
            return true
        }
    }

    inner class OpenFileData(requestParams: Map<String, String>) {
        val file: String? = requestParams["file"]
        val line: Int? = requestParams["line"]?.toIntSilent()
        val offset: Int? = requestParams["offset"]?.toIntSilent()

        private fun String.toIntSilent(): Int? {
            return try {
                this.toInt()
            } catch(e: NumberFormatException) {
                this@OpenFileData.logger.warn("Failed to parse $this parameter: ${e.message}")
                null
            }
        }
    }
}
