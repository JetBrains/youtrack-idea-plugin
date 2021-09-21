package com.github.jk1.ytplugin.scriptsDebug

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.github.jk1.ytplugin.timeTracker.TrackerNotification
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.MalformedJsonException
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.wm.WindowManager
import com.intellij.util.Url
import com.intellij.util.io.addChannelListener
import com.intellij.util.io.handler
import com.intellij.util.io.socketConnection.ConnectionStatus
import io.netty.bootstrap.Bootstrap
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufInputStream
import io.netty.channel.Channel
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.http.*
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory
import io.netty.handler.codec.http.websocketx.WebSocketFrameAggregator
import io.netty.handler.codec.http.websocketx.WebSocketVersion
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import io.netty.util.NetUtil
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.debugger.Vm
import org.jetbrains.debugger.connection.RemoteVmConnection
import org.jetbrains.debugger.createDebugLogger
import org.jetbrains.ide.BuiltInServerManager
import org.jetbrains.io.NettyUtil
import org.jetbrains.io.SimpleChannelInboundHandlerAdapter
import org.jetbrains.io.webSocket.WebSocketProtocolHandler
import org.jetbrains.io.webSocket.WebSocketProtocolHandshakeHandler
import org.jetbrains.wip.BrowserWipVm
import org.jetbrains.wip.WipVm
import org.jetbrains.wip.protocol.inspector.DetachedEventData
import java.awt.Window
import java.net.InetSocketAddress
import java.net.URI
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.Base64.getEncoder

open class WipConnection : RemoteVmConnection<WipVm>() {

    private var currentPageTitle: String? = null
    private val DEBUG_ADDRESS_ENDPOINT = "/api/debug/scripts/json"
    val url: Url? = null

    var pageUrl: String? = null
    var webSocketDebuggerUrl: String? = null
    var title: String? = null
    var type: String? = null
    var id: String? = null

    private var webSocketDebuggerEndpoint: String? = null
    private var webSocketPrefix: String? = null

    val logger: Logger get() = Logger.getInstance("com.github.jk1.ytplugin")

    private val String.b64Encoded: String
        get() = getEncoder().encodeToString(this.toByteArray(StandardCharsets.UTF_8))

    protected open fun createBootstrap() = BuiltInServerManager.getInstance().createClientBootstrap()

    override fun createBootstrap(address: InetSocketAddress, vmResult: AsyncPromise<WipVm>): Bootstrap {
        return createBootstrap().handler {
            val repository = getYouTrackRepo()
            if (repository != null && URI(repository.url).scheme == HttpScheme.HTTPS.toString()) {
                val h = SslContextBuilder.forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE).build()
                it.pipeline().addLast(h.newHandler(NioSocketChannel().alloc(), address.hostName, address.port))
            }
            it.pipeline()
                .addLast(HttpClientCodec(), HttpObjectAggregator(1048576 * 10), createChannelHandler(address, vmResult))
        }
    }

    protected open fun createChannelHandler(address: InetSocketAddress, vmResult: AsyncPromise<WipVm>): ChannelHandler {
        return object : SimpleChannelInboundHandlerAdapter<FullHttpResponse>() {
            override fun channelActive(context: ChannelHandlerContext) {
                super.channelActive(context)
                logger.debug("Channel is active")
                obtainDebugAddress(address, context, vmResult)
            }

            override fun messageReceived(context: ChannelHandlerContext, message: FullHttpResponse) {
                try {
                    context.pipeline().remove(this)
                    connectToPage(context, address, message.content(), vmResult)
                } catch (e: Throwable) {
                    handleExceptionOnGettingWebSockets(e, vmResult)
                }
            }

            override fun exceptionCaught(context: ChannelHandlerContext, cause: Throwable) {
                vmResult.setError(cause)
                context.close()
            }
        }
    }

    protected fun handleExceptionOnGettingWebSockets(e: Throwable, vmResult: AsyncPromise<WipVm>) {
        if (e is MalformedJsonException) {
            var message = "Invalid response from the remote host"
            val host = address?.hostName
            if (host != null && !NetUtil.isValidIpV4Address(host) && !NetUtil.isValidIpV6Address(host) &&
                host != "localhost" && host != "localhost6") {
                message += "Invalid connection to the hostname $host"
            }
            vmResult.setError(message)
        } else {
            vmResult.setError(e)
        }
    }

    private fun notifyIfDebuggerConnectionError() {
        val repo = getYouTrackRepo()
        if (repo == null) {
            val note = "The YouTrack Integration plugin has not been configured to connect with a YouTrack site"
            val trackerNote = TrackerNotification()
            trackerNote.notify(note, NotificationType.ERROR)
        } else if (webSocketDebuggerUrl == null) {
            val note =
                "The debug operation requires that you have permission to update at least one project in YouTrack"
            val trackerNote = TrackerNotification()
            trackerNote.notify(note, NotificationType.ERROR)
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
                logger.error("Unable to get the window", e)
            }

            if (window != null && window!!.isEnabled) {
                activeProject = project
            }

        }
        return activeProject
    }

    protected fun obtainDebugAddress(
        address: InetSocketAddress,
        context: ChannelHandlerContext,
        vmResult: AsyncPromise<WipVm>
    ) {

        val repository = getYouTrackRepo()
        val path = if (repository != null) URI(repository.url).path else ""

        val request = DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "$path$DEBUG_ADDRESS_ENDPOINT")
        request.headers().set(HttpHeaderNames.HOST, address.toHttpHeaderHostField())
        request.headers().set(HttpHeaderNames.ACCEPT, "*/*")
        request.headers().set(HttpHeaderNames.AUTHORIZATION, "Basic ${"${repository?.username}:${repository?.password}".b64Encoded}")

        logger.debug("Request for the acquiring debug address is formed: ${request.uri()}")

        context.channel().writeAndFlush(request).addChannelListener {
            if (!it.isSuccess) {
                logger.debug("Request unsuccessful: ${it.cause()}")
                vmResult.setError(it.cause())
            }
        }
    }

    private fun getYouTrackRepo(): YouTrackServer? {
        val repositories = getActiveProject()?.let {
            ComponentAware.of(it).taskManagerComponent.getAllConfiguredYouTrackRepositories()
        }
        if (repositories != null && repositories.isNotEmpty()) {
            logger.debug("Obtained youtrack repo: ${repositories.first().url}")
            return repositories.first()
        } else {
            logger.debug("Failed to obtain youtrack repo")
        }
        return null
    }

    override fun connectedAddressToPresentation(address: InetSocketAddress, vm: Vm): String {
        return "${super.connectedAddressToPresentation(address, vm)}${currentPageTitle?.let { " \u2013 $it" } ?: ""}"
    }

    protected open fun connectToPage(
        context: ChannelHandlerContext, address: InetSocketAddress,
        connectionsJson: ByteBuf, result: AsyncPromise<WipVm>
    ): Boolean {
        result.onError {
            logger.debug("\"$it\"", "Error")
        }

        if (!connectionsJson.isReadable) {
            result.setError("Malformed response")
            logger.debug("Attempt to receive debug address: " +
                    "${connectionsJson.readCharSequence(connectionsJson.readableBytes(), Charset.forName("utf-8"))}"
            )
            return true
        }

        processDebuggerConnectionJson(connectionsJson)
        logger.debug("YouTrack debug address obtained: $webSocketDebuggerUrl")

        notifyIfDebuggerConnectionError()
        return !processConnection(context, result)
    }

    private fun processDebuggerConnectionJson(connectionsJson: ByteBuf) {
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
                    "webSocketDebuggerEndpoint" -> webSocketDebuggerEndpoint = reader.nextString()
                    "websocketPrefix" -> webSocketPrefix = reader.nextString()
                    "id" -> id = reader.nextString()
                    else -> reader.skipValue()
                }
            }
            reader.endObject()
        }
        if (webSocketDebuggerEndpoint != null){
            webSocketDebuggerUrl = constructWebsocketDebuggerUrl()
        }
    }

    private fun constructWebsocketDebuggerUrl(): String {
        return "$webSocketPrefix${URI(getYouTrackRepo()?.url).authority}$webSocketDebuggerEndpoint"
    }

    protected open fun processConnection(
        context: ChannelHandlerContext,
        result: AsyncPromise<WipVm>
    ): Boolean {
        if (webSocketDebuggerUrl != null) {
            logger.debug("Connect debugger for ${URI(getYouTrackRepo()?.url).authority}")
            connectDebugger(context, result)
            return true
        } else {
            result.setError("Another debugger is attached, please ensure that configuration is stopped or restart application to force detach")
            logger.debug("Another debugger is attached, please ensure that configuration is stopped or restart application to force detach")
        }
        return true
    }

    protected open fun connectDebugger(
        context: ChannelHandlerContext,
        result: AsyncPromise<WipVm>
    ) {
        val handshaker = WebSocketClientHandshakerFactory.newHandshaker(
            URI.create(webSocketDebuggerUrl!!),
            WebSocketVersion.V13,
            null,
            false,
            null,
            100 * 1024 * 1024
        )
        val channel = context.channel()
        val vm = BrowserWipVm(
            debugEventListener,
            webSocketDebuggerUrl,
            channel,
            createDebugLogger("js.debugger.wip.log", "")
        )
        vm.title = title
        vm.commandProcessor.eventMap.add(DetachedEventData.TYPE) {
            if (it.reason() == "targetCrashed") {
                close("${ConnectionStatus.DISCONNECTED.statusText} (Debugger crashed)", ConnectionStatus.DISCONNECTED)
            } else {
                close(
                    "${ConnectionStatus.DISCONNECTED.statusText} (Debugger already opened)",
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

}

internal fun InetSocketAddress.toHttpHeaderHostField(): String =
    "${(this as? InetSocketAddress)?.hostName ?: hostString}:$port"