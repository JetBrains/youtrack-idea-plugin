package com.github.jk1.ytplugin.workflowsDebugConfiguration

import com.jetbrains.debugger.wip.WipRemoteVmConnection
import com.intellij.javascript.debugger.JSDebuggerBundle
import com.intellij.openapi.util.Condition
import com.intellij.openapi.util.Conditions
import com.intellij.util.io.connectRetrying
import com.intellij.util.io.handler
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.FullHttpResponse
import io.netty.handler.codec.http.HttpClientCodec
import io.netty.handler.codec.http.HttpObjectAggregator
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.isPending
import org.jetbrains.io.NettyUtil
import org.jetbrains.io.SimpleChannelInboundHandlerAdapter
import org.jetbrains.wip.WipVm
import java.net.InetSocketAddress

class WipConnection : WipRemoteVmConnection() {

    init {
        println("hello")
    }

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

                            sendGetJson(address, context, result)
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