package com.github.jk1.ytplugin.scriptsDebug

import com.github.jk1.ytplugin.logger
import com.intellij.util.Urls
import com.intellij.util.io.addChannelListener
import com.intellij.util.io.readUtf8
import io.netty.channel.Channel
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.catchError
import org.jetbrains.debugger.DebugEventListener
import org.jetbrains.debugger.MessagingLogger
import org.jetbrains.debugger.StandaloneVmHelper
import org.jetbrains.debugger.doCloseChannel
import org.jetbrains.io.JsonReaderEx
import org.jetbrains.jsonProtocol.Request
import org.jetbrains.rpc.LOG
import org.jetbrains.wip.WipVm
import org.jetbrains.wip.WipWorkerManager
import java.nio.charset.Charset

abstract class StandaloneDebuggerWipVm(
    tabListener: DebugEventListener,
    url: String?,
    channel: Channel,
    val debugMessageQueue: MessagingLogger? = null,
    workerManagerFactory: (WipVm) -> WipWorkerManager = ::WipWorkerManager
) : WipVm(tabListener, workerManagerFactory = workerManagerFactory) {

    private val vmHelper = object : StandaloneVmHelper(this, commandProcessor, channel) {
        override fun closeChannel(channel: Channel, promise: AsyncPromise<Any?>) {
            promise.catchError {
                if (channel.isActive) {
                    channel
                        .writeAndFlush(CloseWebSocketFrame())
                        .addChannelListener { doCloseChannel(channel, promise) }
                } else {
                    promise.setResult(null)
                }
            }
        }
    }

    override val attachStateManager: StandaloneVmHelper = vmHelper

    init {
        debugMessageQueue?.closeOnChannelClose(channel)
        currentUrl = url?.let { Urls.newFromEncoded(url) }
    }

    open fun textFrameReceived(message: TextWebSocketFrame) {
        debugMessageQueue?.add(message.content(), "IN")
        logger.info("Debugger text frame received: ${message.content()
            .readCharSequence(message.content().readableBytes(), Charset.forName("utf-8"))}")
        try {
            commandProcessor.processIncomingJson(JsonReaderEx(message.content().readUtf8()))
        } catch (e: Exception) {
            LOG.error(e)
        } finally {
            message.release()
        }
    }

    override fun write(message: Request<*>): Boolean {
        val content = message.buffer
        debugMessageQueue?.add(content)
        return vmHelper.write(TextWebSocketFrame(content))
    }
}