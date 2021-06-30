package com.github.jk1.ytplugin.scriptsDebug

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.logger
import com.github.jk1.ytplugin.tasks.NoYouTrackRepositoryException
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.github.jk1.ytplugin.timeTracker.TrackerNotification
import com.github.jk1.ytplugin.whenActive
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.Condition
import com.intellij.openapi.util.Conditions
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.SmartList
import com.intellij.util.Urls
import com.intellij.util.io.addChannelListener
import com.intellij.util.io.connectRetrying
import com.intellij.util.io.handler
import com.intellij.util.io.socketConnection.ConnectionStatus
import com.intellij.util.proxy.ProtocolDefaultPorts
import com.jetbrains.debugger.wip.PageConnection
import com.jetbrains.debugger.wip.WipRemoteVmConnection
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufInputStream
import io.netty.channel.Channel
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.*
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory
import io.netty.handler.codec.http.websocketx.WebSocketFrameAggregator
import io.netty.handler.codec.http.websocketx.WebSocketVersion
import io.netty.util.CharsetUtil
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.isPending
import org.jetbrains.debugger.MessagingLogger
import org.jetbrains.debugger.connection.chooseDebuggee
import org.jetbrains.io.NettyUtil
import org.jetbrains.io.SimpleChannelInboundHandlerAdapter
import org.jetbrains.io.webSocket.WebSocketProtocolHandler
import org.jetbrains.io.webSocket.WebSocketProtocolHandshakeHandler
import org.jetbrains.wip.WipVm
import org.jetbrains.wip.protocol.inspector.DetachedEventData
import java.awt.Desktop
import java.awt.Window
import java.net.InetSocketAddress
import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.*


class WipConnection : WipRemoteVmConnection() {

    private var currentPageTitle: String? = null

    @Volatile
    private var connectionsData: ByteBuf? = null

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
        @Synchronized
        fun connectToWebSocket() {
            when {
                isBaseurlMatchingActual() -> {
                    super.doOpen(
                        result,
                        InetSocketAddress(
                            URI(webSocketDebuggerUrl!!).host,
                            if (URI(webSocketDebuggerUrl).port <= 0) ProtocolDefaultPorts.SSL else URI(
                                webSocketDebuggerUrl!!
                            ).port
                        ),
                        stopCondition
                    )

                }
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
                            getJsonInfo(connectionsData!!)
                            connectToWebSocket()
                        } catch (e: Throwable) {
                            logger.debug("Malformed json response: ${e.message} with content: ${connectionsData?.readCharSequence(
                                connectionsData!!.writerIndex(),
                                CharsetUtil.UTF_8).toString()}")
                        }
                    }

                    override fun exceptionCaught(context: ChannelHandlerContext, cause: Throwable) {
                        result.setError(cause)
                        context.close()
                    }
                }
            )
        }.connectRetrying(address, maxAttemptCount, combinedCondition)

        if (connectResult.channel == null && result.isPending) {
            result.setError("Cannot connect to $address")
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
                logger.error("Unable to get the window: ${e.message}")
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
            vmResult.setError("YouTrack server integration is not configured yet")
        }

    }

    override fun createChannelHandler(address: InetSocketAddress, vmResult: AsyncPromise<WipVm>): ChannelHandler {
        return object : SimpleChannelInboundHandlerAdapter<FullHttpResponse>() {
            override fun channelActive(context: ChannelHandlerContext) {
                super.channelActive(context)
                try {
                    context.pipeline().remove(this)
                    connectDebugger(PageConnection(webSocketDebuggerUrl, title, type, webSocketDebuggerUrl, id, address),
                        context, vmResult,null)
                } catch (e: Throwable) {
                    handleExceptionOnGettingWebSockets(e, vmResult)
                }
            }

            override fun messageReceived(context: ChannelHandlerContext, message: FullHttpResponse) {

            }

            override fun exceptionCaught(context: ChannelHandlerContext, cause: Throwable) {
                vmResult.setError(cause)
                context.close()
            }
        }
    }

    fun getJsonInfo(connectionsJson: ByteBuf) {

        if (!connectionsJson.isReadable) {
            logger.debug("Malformed response: $connectionsJson")
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
                    "title" -> title = reader.nextString()
                    "type" -> type = reader.nextString()
                    "webSocketDebuggerUrl" -> webSocketDebuggerUrl = reader.nextString()
                    "id" -> id = reader.nextString()
                    else -> reader.skipValue()
                }
            }
            reader.endObject()
        }
        notifyUrlsShouldMatch()
    }


    private fun getYouTrackRepo() : YouTrackServer? {
        val repositories =
            getActiveProject()?.let { ComponentAware.of(it).taskManagerComponent.getAllConfiguredYouTrackRepositories() }
        if (repositories != null && repositories.isNotEmpty()) {
            return repositories.first()
        }
        return null
    }

    private fun isBaseurlMatchingActual(): Boolean {
        return webSocketDebuggerUrl != null && getYouTrackRepo() != null  &&
                URI(webSocketDebuggerUrl).authority == URI(getYouTrackRepo()?.url).authority
    }

    private fun notifyUrlsShouldMatch() {
        val repo = getYouTrackRepo()
        when {
            !isBaseurlMatchingActual() && repo != null && webSocketDebuggerUrl != null -> {
                val note = "To start using scripts debugger Base URL of the YouTrack instance should match its actual URL"
                val trackerNote = TrackerNotification()
                trackerNote.notifyWithHelper(note, NotificationType.WARNING, object : AnAction("Settings"), DumbAware {
                    override fun actionPerformed(event: AnActionEvent) {
                        event.whenActive {
                            val desktop: Desktop? = if (Desktop.isDesktopSupported()) Desktop.getDesktop() else null
                            if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
                                try {
                                    val repository =  getYouTrackRepo()
                                    desktop.browse(URI("${repository?.url}/admin/settings"))
                                } catch (e: java.lang.Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    }
                })
            }
            repo == null -> {
                val note = "YouTrack server integration is not configured yet"
                val trackerNote = TrackerNotification()
                trackerNote.notify(note, NotificationType.WARNING)
            }
            webSocketDebuggerUrl == null -> {
                val note =
                    "Please check your permissions, you should be able to update any project to debug scripts"
                val trackerNote = TrackerNotification()
                trackerNote.notify(note, NotificationType.WARNING)
            }
        }
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
                    "${ConnectionStatus.DISCONNECTED.statusText} (connection was closed)",
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

                override fun exceptionCaught(context: ChannelHandlerContext, cause: Throwable) {
                    result.setError(cause)
                    context.fireExceptionCaught(cause)
                }
            },
            WebSocketFrameAggregator(NettyUtil.MAX_CONTENT_LENGTH),
            object : WebSocketProtocolHandler() {
                override fun textFrameReceived(channel: Channel, message: TextWebSocketFrame) {
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

    override fun detachAndClose(): Promise<*> {
        if (connectionsData != null && connectionsData!!.refCnt() > 0) {
            connectionsData!!.release()
        }
        return super.detachAndClose()
    }
}