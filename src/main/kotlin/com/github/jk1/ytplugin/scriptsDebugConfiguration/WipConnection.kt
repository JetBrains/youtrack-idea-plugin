package com.github.jk1.ytplugin.scriptsDebugConfiguration

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.logger
import com.github.jk1.ytplugin.tasks.NoYouTrackRepositoryException
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.intellij.javascript.debugger.JSDebuggerBundle
import com.intellij.openapi.application.ApplicationManager
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
import com.intellij.util.io.socketConnection.ConnectionStatus
import io.netty.channel.Channel
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory
import io.netty.handler.codec.http.websocketx.WebSocketFrameAggregator
import io.netty.handler.codec.http.websocketx.WebSocketVersion
import org.jetbrains.io.webSocket.WebSocketProtocolHandler
import org.jetbrains.io.webSocket.WebSocketProtocolHandshakeHandler
import org.jetbrains.wip.protocol.inspector.DetachedEventData
import java.nio.charset.StandardCharsets
import java.util.*


class WipConnection : WipRemoteVmConnection() {

    private var currentPageTitle: String? = null

    @Volatile
    private var connectionsData: ByteBuf? = null

    private var pageUrl: String? = null
    private var webSocketDebuggerUrl: String? = null
    private var title: String? = null
    private var type: String? = null
    private var id: String? = null

    private val String.b64Encoded: String
        get() = Base64.getEncoder().encodeToString(this.toByteArray(StandardCharsets.UTF_8))

    override fun doOpen(result: AsyncPromise<WipVm>, address: InetSocketAddress, stopCondition: Condition<Void>?) {
        val maxAttemptCount = if (stopCondition == null) NettyUtil.DEFAULT_CONNECT_ATTEMPT_COUNT else -1
        val resultRejected = Condition<Void> { result.state == Promise.State.REJECTED }
        val combinedCondition = Conditions.or(stopCondition ?: Conditions.alwaysFalse(), resultRejected)
        fun connectToWebSocket() {
            if (webSocketDebuggerUrl != null) {
                super.doOpen(
                    result,
                    InetSocketAddress(URI(webSocketDebuggerUrl!!).host, URI(webSocketDebuggerUrl!!).port),
                    stopCondition
                )
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

            var window: Window? = null
            // required to avoid threads exception
            try {
                ApplicationManager.getApplication().invokeAndWait {
                    window = WindowManager.getInstance().suggestParentWindow(project)
                }
            } catch (e: Exception) {
                logger.error("IUnable to get the window: ${e.message}")
            }

            if (window != null && window!!.isActive) {
                activeProject = project
            }

        }
        return activeProject
    }

    fun formJsonRequest(address: InetSocketAddress, context: ChannelHandlerContext, vmResult: AsyncPromise<WipVm>) {

        // get active project to detect YouTrack repo
        val activeProject = getActiveProject()

        // todo cloud
        val request = DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/api/scripts/debug/json")
        request.headers().set(HttpHeaderNames.HOST, "${address.hostString}:${address.port}")
        request.headers().set(HttpHeaderNames.ACCEPT, "*/*")

        try {
            val repositories =
                activeProject?.let { ComponentAware.of(it).taskManagerComponent.getAllConfiguredYouTrackRepositories() }

            val password = if (repositories != null && repositories.isNotEmpty()) repositories[0].password else ""
            val username = if (repositories != null && repositories.isNotEmpty()) repositories[0].username else ""

            val authCredentials = "${username}:${password}".b64Encoded

            request.headers().set(HttpHeaderNames.AUTHORIZATION, "Basic $authCredentials")

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

    fun getJsonInfo(
        connectionsJson: ByteBuf,
        result: AsyncPromise<WipVm>
    ) {

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
                    "devtoolsFrontendUrl" -> pageUrl = reader.nextString()
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

    override fun connectToPage(
        context: ChannelHandlerContext,
        address: InetSocketAddress,
        connectionsJson: ByteBuf,
        result: AsyncPromise<WipVm>
    ): Boolean {

        logger.debug("On connection to page: $connectionsJson")
        result.onError {
            logger.debug("Error: $it")
        }

        val pageConnections = SmartList<PageConnection>()

        // read is allowed if the user can update any project  (e.g. he can read this workflow and write the similar one for his own projects);
        // read project permission for any project is not sufficient as reading workflows without being able to update
        // any of them seems to be a security leak
        if (webSocketDebuggerUrl == null)
            result.setError("Please check your permissions, you should be able to update any project to debug scripts")

        pageConnections.add(PageConnection(pageUrl, title, type, webSocketDebuggerUrl, id, address))
        return !processPageConnections(context,null, pageConnections, result)
    }

    override fun connectDebugger(
        page: PageConnection,
        context: ChannelHandlerContext,
        result: AsyncPromise<WipVm>,
        debugMessageQueue: MessagingLogger?
    ) {
        val handshaker = WebSocketClientHandshakerFactory.newHandshaker(
            URI.create(page.webSocketDebuggerUrl!!),
            WebSocketVersion.V13,
            null,
            false,
            null,
            100 * 1024 * 1024
        )
        val channel = context.channel()
        val vm = DebuggerWipVm(debugEventListener, page.url, channel)
        vm.title = page.title
        vm.commandProcessor.eventMap.add(DetachedEventData.TYPE) {
            if (it.reason() == "targetCrashed") {
                close("${ConnectionStatus.DISCONNECTED.statusText} (debugger crashed)", ConnectionStatus.DISCONNECTED)
            } else {
                close(
                    "${ConnectionStatus.DISCONNECTED.statusText} (debugger was closed or Web Inspector was opened)",
                    ConnectionStatus.DETACHED
                )
            }
        }

        channel.pipeline().addLast(
            object : WebSocketProtocolHandshakeHandler(handshaker) {
                override fun completed() {
                    vm.initDomains()
                    result.setResult(vm)
                    vm.ready()
                }

                override fun exceptionCaught(
                    @Suppress("NAME_SHADOWING") context: ChannelHandlerContext,
                    cause: Throwable
                ) {
                    result.setError(cause)
                    context.fireExceptionCaught(cause)
                }
            },
            WebSocketFrameAggregator(NettyUtil.MAX_CONTENT_LENGTH),
            object : WebSocketProtocolHandler() {
                override fun textFrameReceived(
                    @Suppress("NAME_SHADOWING") channel: Channel,
                    message: TextWebSocketFrame
                ) {
                    vm.textFrameReceived(message)
                }
            }
        )

        handshaker.handshake(channel).addChannelListener {
            if (!it.isSuccess) {
                context.fireExceptionCaught(it.cause())
            }
        }
    }

    override fun processPageConnections(
        context: ChannelHandlerContext,
        debugMessageQueue: MessagingLogger?,
        pageConnections: List<PageConnection>,
        result: AsyncPromise<WipVm>,
    ): Boolean {
        val debuggablePages = SmartList<PageConnection>()

        for (p in pageConnections) {
            if (url == null) {
                debuggablePages.add(p)
            } else if (Urls.equals(url, Urls.newFromEncoded(p.url!!), SystemInfo.isFileSystemCaseSensitive, true)) {
                connectDebugger(p, context, result, null)
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

                connectDebugger(it, context, result, null)
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