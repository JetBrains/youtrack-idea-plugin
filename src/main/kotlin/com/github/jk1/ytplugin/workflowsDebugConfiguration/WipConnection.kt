package com.github.jk1.ytplugin.workflowsDebugConfiguration

import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.jetbrains.debugger.wip.WipRemoteVmConnection
import com.intellij.javascript.debugger.JSDebuggerBundle
import com.intellij.openapi.util.Condition
import com.intellij.openapi.util.Conditions
import com.intellij.util.SmartList
import com.intellij.util.io.addChannelListener
import com.intellij.util.io.connectRetrying
import com.intellij.util.io.handler
import com.jetbrains.debugger.wip.PageConnection
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufInputStream
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.*
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.isPending
import org.jetbrains.debugger.createDebugLogger
import org.jetbrains.io.NettyUtil
import org.jetbrains.io.SimpleChannelInboundHandlerAdapter
import org.jetbrains.wip.WipVm
import java.net.InetSocketAddress

class WipConnection : WipRemoteVmConnection() {

    private val JSON_API_ENDPOINT = "/api/workflowsinspector/json"

    @Volatile
    private var connectionsData: ByteBuf? = null

    override fun doOpen(result: AsyncPromise<WipVm>, address: InetSocketAddress, stopCondition: Condition<Void>?) {
        val maxAttemptCount = if (stopCondition == null) NettyUtil.DEFAULT_CONNECT_ATTEMPT_COUNT else -1
        val resultRejected = Condition<Void> { result.state == Promise.State.REJECTED }
        val combinedCondition = Conditions.or(stopCondition ?: Conditions.alwaysFalse(), resultRejected)
        fun connectToWebSocket() {
            super.doOpen(result, address, stopCondition)
        }
        val connectResult = createBootstrap().handler {
            it.pipeline().addLast(
                    HttpClientCodec(),
                    HttpObjectAggregator(1048576 * 10),
                    object : SimpleChannelInboundHandlerAdapter<FullHttpResponse>() {
                        override fun channelActive(context: ChannelHandlerContext) {
                            super.channelActive(context)
                            sendJson(address, context, result)
                        }

                        override fun messageReceived(context: ChannelHandlerContext, message: FullHttpResponse) {
                            try {
                                context.pipeline().remove(this)
                                context.close()
                                connectionsData = message.content().copy()
                                connectToWebSocket()
                            }
                            catch (e: Throwable) {
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

    fun sendJson(address: InetSocketAddress, context: ChannelHandlerContext, vmResult: AsyncPromise<WipVm>) {
        val request = DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, JSON_API_ENDPOINT)
        request.headers().set(HttpHeaderNames.HOST, address.hostString)
        request.headers().set(HttpHeaderNames.ACCEPT, "*/*")
        request.headers().set(HttpHeaderNames.AUTHORIZATION, "Basic cm9vdDpyb290")

        context.channel().writeAndFlush(request).addChannelListener {
            if (!it.isSuccess) {
                vmResult.setError(it.cause())
            }
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

        if (!connectionsJson.isReadable) {
            result.setError(JSDebuggerBundle.message("error.websocket.malformed.message"))
            return true
        }

        val reader = JsonReader(ByteBufInputStream(connectionsJson).reader())
        if (reader.peek() == JsonToken.BEGIN_ARRAY) {
            reader.beginArray()
        }
        val pageConnections = SmartList<PageConnection>()
        while (reader.hasNext() && reader.peek() != JsonToken.END_DOCUMENT) {
            reader.beginObject()
            var pageUrl: String? = null
            var webSocketDebuggerUrl: String? = null
            var title: String? = null
            var type: String? = null
            var id: String? = null
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

            pageConnections.add(PageConnection(pageUrl, title, type, webSocketDebuggerUrl, id, address))
        }

        return !processPageConnections(context, debugMessageQueue, pageConnections, result)
    }

    override fun createChannelHandler(address: InetSocketAddress, vmResult: AsyncPromise<WipVm>): ChannelHandler {
        return object : SimpleChannelInboundHandlerAdapter<FullHttpResponse>() {
            override fun channelActive(context: ChannelHandlerContext) {
                super.channelActive(context)
                try {
                    context.pipeline().remove(this)
                    connectToPage(context, address, connectionsData!!, vmResult)
                }
                catch (e: Throwable) {
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

    override fun detachAndClose(): Promise<*> {
        if (connectionsData != null && connectionsData!!.refCnt() > 0) {
            connectionsData!!.release()
        }
        return super.detachAndClose()
    }

}