package com.github.jk1.ytplugin.navigator

import com.github.jk1.ytplugin.common.components.ComponentAware
import com.intellij.openapi.project.Project
import fi.iki.elonen.NanoHTTPD


class ConnectionHandler(override val project: Project, port: Int) : NanoHTTPD("127.0.0.1", port), ComponentAware {

    val resources = listOf(OpenFileResource(project))

    override fun serve(session: IHTTPSession): Response {
        val handler = resources.filter { it.canHandle(session) }.firstOrNull()
        return handler?.handle(session) ?: customResponse(Response.Status.NOT_IMPLEMENTED)
    }

    interface Resource {
        fun canHandle(session: IHTTPSession) : Boolean
        fun handle(session: IHTTPSession) : Response
    }
}