package com.github.jk1.ytplugin.workflowsDebugConfiguration

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.tasks.NoYouTrackRepositoryException
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.intellij.javascript.debugger.JSDebuggerBundle
import com.intellij.openapi.util.Condition
import com.intellij.openapi.util.Conditions
import com.intellij.openapi.util.SystemInfo
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.SmartList
import com.intellij.util.Urls
import com.intellij.util.io.addChannelListener
import com.intellij.util.io.connectRetrying
import com.intellij.util.io.handler
import com.jetbrains.debugger.wip.PageConnection
import com.jetbrains.debugger.wip.WipRemoteVmConnection
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufInputStream
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.*
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.isPending
import org.jetbrains.debugger.MessagingLogger
import org.jetbrains.debugger.connection.chooseDebuggee
import org.jetbrains.debugger.createDebugLogger
import org.jetbrains.io.NettyUtil
import org.jetbrains.io.SimpleChannelInboundHandlerAdapter
import org.jetbrains.wip.WipVm
import java.net.InetSocketAddress
import java.net.URI

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.WindowManager

import com.intellij.openapi.project.ProjectManager
import java.awt.Window


class WipConnection : WipRemoteVmConnection() {

    private var currentPageTitle: String? = null

    @Volatile
    private var connectionsData: ByteBuf? = null

    private var pageUrl: String? = null
    private var webSocketDebuggerUrl: String? = null
    private var title: String? = null
    private var type: String? = null
    private var id: String? = null

    override fun doOpen(result: AsyncPromise<WipVm>, address: InetSocketAddress, stopCondition: Condition<Void>?) {
        val maxAttemptCount = if (stopCondition == null) NettyUtil.DEFAULT_CONNECT_ATTEMPT_COUNT else -1
        val resultRejected = Condition<Void> { result.state == Promise.State.REJECTED }
        val combinedCondition = Conditions.or(stopCondition ?: Conditions.alwaysFalse(), resultRejected)
        fun connectToWebSocket() {
            if (webSocketDebuggerUrl != null) {
                super.doOpen(result, InetSocketAddress(URI(webSocketDebuggerUrl!!).host, URI(webSocketDebuggerUrl!!).port), stopCondition)
            } else {
                result.setError("Please check your permissions to debug")
            }
        }

        val connectResult = createBootstrap().handler {
            it.pipeline().addLast(
                    HttpClientCodec(),
                    HttpObjectAggregator(1048576 * 10),
                    object : SimpleChannelInboundHandlerAdapter<FullHttpResponse>() {
                        override fun channelActive(context: ChannelHandlerContext) {
                            super.channelActive(context)
                            formJsonRequest(address, context, result)
                        }

                        override fun messageReceived(context: ChannelHandlerContext, message: FullHttpResponse) {
                            try {
                                context.pipeline().remove(this)
                                context.close()
                                connectionsData = message.content().copy()
                                getJsonInfo(connectionsData!!, result)
                                connectToWebSocket()
                            } catch (e: Throwable) {
                                handleExceptionOnGettingWebSockets(e, result)
                            }
                        }

                        @Suppress("OverridingDeprecatedMember")
                        override fun exceptionCaught(context: ChannelHandlerContext, cause: Throwable) {
                            result.setError(cause)
                            context.close()
                        }
                    }
            )
        }.connectRetrying(address, maxAttemptCount, combinedCondition)

        if (connectResult.channel == null && result.isPending) {
            result.setError(JSDebuggerBundle.message("error.connection.address", address))
        }
    }

    private fun getActiveProject(): Project? {
        val projects = ProjectManager.getInstance().openProjects
        var activeProject: Project? = null
        for (project in projects) {
            val window: Window? = WindowManager.getInstance().suggestParentWindow(project)
            if (window != null && window.isActive) {
                activeProject = project
            }
        }
        return activeProject
    }

    fun formJsonRequest(address: InetSocketAddress, context: ChannelHandlerContext, vmResult: AsyncPromise<WipVm>) {

        // get active project to detect YouTrack repo
        val activeProject = getActiveProject()

        val request = DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/api/workflowsinspector/json")
        request.headers().set(HttpHeaderNames.HOST, "${address.hostString}:${address.port}")
        request.headers().set(HttpHeaderNames.ACCEPT, "*/*")

        try {
            val repositories = activeProject?.let { ComponentAware.of(it).taskManagerComponent.getAllConfiguredYouTrackRepositories() }
            val token = if (repositories != null && repositories.isNotEmpty()) repositories[0].password else ""

            request.headers().set(HttpHeaderNames.AUTHORIZATION, "Bearer $token")
            request.headers().set(HttpHeaderNames.ACCEPT, "application/json")
            context.channel().writeAndFlush(request).addChannelListener {
                if (!it.isSuccess) {
                    vmResult.setError(it.cause())
                }
            }
        } catch (e: NoYouTrackRepositoryException) {
            vmResult.setError("Please check your permissions to debug")
        }

    }

    override fun createChannelHandler(address: InetSocketAddress, vmResult: AsyncPromise<WipVm>): ChannelHandler {

        return object : SimpleChannelInboundHandlerAdapter<FullHttpResponse>() {
            override fun channelActive(context: ChannelHandlerContext) {
                super.channelActive(context)
                try {
                    context.pipeline().remove(this)
                    connectToPage(context, address, connectionsData!!, vmResult)
                } catch (e: Throwable) {
                    handleExceptionOnGettingWebSockets(e, vmResult)
                }
            }

            override fun messageReceived(context: ChannelHandlerContext, message: FullHttpResponse) {

            }

            @Suppress("OverridingDeprecatedMember")
            override fun exceptionCaught(context: ChannelHandlerContext, cause: Throwable) {
                vmResult.setError(cause)
                context.close()
            }
        }
    }

    fun getJsonInfo(connectionsJson: ByteBuf,
                    result: AsyncPromise<WipVm>) {

        if (!connectionsJson.isReadable) {
            result.setError(JSDebuggerBundle.message("error.websocket.malformed.message"))
            return
        }

        val reader = JsonReader(ByteBufInputStream(connectionsJson).reader())
        if (reader.peek() == JsonToken.BEGIN_ARRAY) {
            reader.beginArray()
        }
        while (reader.hasNext() && reader.peek() != JsonToken.END_DOCUMENT) {
            reader.beginObject()
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "url" -> pageUrl = reader.nextString()
                    "title" -> title = reader.nextString()
                    "type" -> type = reader.nextString()
                    "webSocketDebuggerUrl" -> webSocketDebuggerUrl = reader.nextString()
                    "id" -> id = reader.nextString()
                    else -> reader.skipValue()
                }
            }
            reader.endObject()
        }
    }

    override fun connectToPage(context: ChannelHandlerContext,
                               address: InetSocketAddress,
                               connectionsJson: ByteBuf,
                               result: AsyncPromise<WipVm>): Boolean {

        val debugMessageQueue = createDebugLogger("js.debugger.wip.log", debugLogSuffix ?: "")
        debugMessageQueue?.let { logger ->
            logger.add(connectionsJson, "IN")
            result.onError {
                logger.add("\"$it\"", "Error")
                logger.close()
            }
        }

        val pageConnections = SmartList<PageConnection>()

        if (webSocketDebuggerUrl == null)
            result.setError("Please check your permissions to debug")

        pageConnections.add(PageConnection(pageUrl, title, type, webSocketDebuggerUrl, id, address))
        return !processPageConnections(context, debugMessageQueue, pageConnections, result)
    }

    override fun processPageConnections(context: ChannelHandlerContext,
                                        debugMessageQueue: MessagingLogger?,
                                        pageConnections: List<PageConnection>,
                                        result: AsyncPromise<WipVm>): Boolean {
        val debuggablePages = SmartList<PageConnection>()

        for (p in pageConnections) {
            if (url == null) {
                debuggablePages.add(p)
            } else if (Urls.equals(url, Urls.newFromEncoded(p.url!!), SystemInfo.isFileSystemCaseSensitive, true)) {
                connectDebugger(p, context, result, debugMessageQueue)
                return true
            }
        }

        if (url == null) {
            chooseDebuggee(debuggablePages, -1) { item, renderer ->
                renderer.append("${item.title} ${item.url?.let { "($it)" } ?: ""}",
                        if (item.webSocketDebuggerUrl == null) SimpleTextAttributes.GRAYED_ATTRIBUTES else SimpleTextAttributes.REGULAR_ATTRIBUTES)
            }.onSuccess {
                val webSocketDebuggerUrl = it.webSocketDebuggerUrl
                if (webSocketDebuggerUrl == null) {
                    result.setError(JSDebuggerBundle.message("js.debug.another.debugger.attached"))
                    return@onSuccess
                }

                currentPageTitle = it.title

                connectDebugger(it, context, result, debugMessageQueue)
            }
                    .onError { result.setError(it) }
        } else {
            result.setError(JSDebuggerBundle.message("error.connection.no.page", url))
        }
        return true
    }

    override fun detachAndClose(): Promise<*> {
        if (connectionsData != null && connectionsData!!.refCnt() > 0) {
            connectionsData!!.release()
        }
        return super.detachAndClose()
    }
}