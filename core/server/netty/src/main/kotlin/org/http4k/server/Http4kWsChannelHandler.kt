package org.http4k.server

import io.netty.buffer.ByteBufInputStream
import io.netty.buffer.Unpooled
import io.netty.buffer.Unpooled.EMPTY_BUFFER
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelFutureListener.CLOSE
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import io.netty.handler.codec.http.websocketx.WebSocketFrame
import org.http4k.websocket.PushPullAdaptingWebSocket
import org.http4k.websocket.WsConsumer
import org.http4k.websocket.WsMessage
import org.http4k.websocket.WsStatus
import org.http4k.websocket.WsStatus.Companion.NOCODE
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class Http4kWsChannelHandler(
    private val wSocket: WsConsumer,
    private val appExecutor: Executor,
    private val bufferCapacity: Int = 1_000, // beyond this size, apply backpressure to netty
    private val lowWaterMark: Float = 0.5f
) : SimpleChannelInboundHandler<WebSocketFrame>() {
    @Volatile private var websocket: PushPullAdaptingWebSocket? = null
    private var normalClose = false
    private val messageBuffer = ConcurrentLinkedQueue<WebSocketFrame>()
    private val bufferSize = AtomicInteger(0) // because the queue's size getter is O(n)
    private val drainLock = AtomicBoolean()

    override fun handlerAdded(ctx: ChannelHandlerContext) {
        val ws = object : PushPullAdaptingWebSocket() {
            override fun send(message: WsMessage) {
                when (message.mode) {
                    WsMessage.Mode.Text -> ctx.writeAndFlush(TextWebSocketFrame(message.bodyString()))
                    WsMessage.Mode.Binary -> ctx.writeAndFlush(BinaryWebSocketFrame(message.body.stream.use {
                        Unpooled.wrappedBuffer(
                            it.readBytes()
                        )
                    }))
                }
            }

            override fun close(status: WsStatus) {
                ctx.writeAndFlush(CloseWebSocketFrame(status.code, status.description))
                    .addListeners(ChannelFutureListener {
                        normalClose = true
                        websocket?.triggerClose(status)
                    }, CLOSE)
            }
        }

        // Delegate websocket init to the application so as to not block the netty event loop group
        appExecutor.execute {
            wSocket(ws)

            /* Do not register the websocket until the WsConsumer is invoked.
             * Otherwise, there will be a race between draining messages and registering message handlers.
             */
            websocket = ws

            drainBuffer(ctx, ws)
        }
    }

    override fun handlerRemoved(ctx: ChannelHandlerContext) {
        if (!normalClose) {
            ctx.writeAndFlush(EMPTY_BUFFER).addListeners(ChannelFutureListener {
                websocket?.triggerClose(NOCODE)
                websocket = null
            }, CLOSE)
        } else {
            websocket = null
        }

        // Release buffer to prevent memory leak for closed connections
        while(messageBuffer.isNotEmpty()) {
            messageBuffer.poll().release()
            bufferSize.decrementAndGet()
        }
    }

    override fun channelRead0(ctx: ChannelHandlerContext, msg: WebSocketFrame) {
        // Always buffer messages to ensure they are processed in the correct order
        messageBuffer.offer(msg.retain())
        bufferSize.incrementAndGet()

        // if the websocket hasn't been initialized, just buffer the message
        websocket?.let {
            // Apply backpressure to prevent OOM from backed-up buffer
            if (bufferSize.get() >= bufferCapacity) {
                ctx.channel().config().isAutoRead = false
            }

            tryDrainBuffer(ctx, it)
        }
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        websocket?.also {
            appExecutor.execute { it.triggerError(cause) }
        }
    }

    /*
     * It's very important to guarantee websocket frames are processed in order.  If we submit all frames to the
     * executor independently, we risk out-of-order and/or concurrent processing.  Thus, we buffer incoming frames
     * and only allow a single thread to drain the buffer at a time.
     */
    private fun drainBuffer(ctx: ChannelHandlerContext, websocket: PushPullAdaptingWebSocket) {
        try {
            while (messageBuffer.isNotEmpty()) {
                val msg = messageBuffer.poll() ?: return
                val currentSize = bufferSize.decrementAndGet()

                try {
                    when (msg) {
                        is TextWebSocketFrame -> websocket.triggerMessage(WsMessage(msg.text()))
                        is BinaryWebSocketFrame -> websocket.triggerMessage(
                            WsMessage(
                                ByteBufInputStream(msg.content()),
                                WsMessage.Mode.Binary
                            )
                        )

                        is CloseWebSocketFrame -> {
                            msg.retain() // writeAndFlush releases, so the caller might decrement the message count illegally
                            closeWebsocket(ctx, websocket, msg)
                        }
                    }
                } catch (e: Exception) {
                    websocket.triggerError(e)
                } finally {
                    msg.release()
                }

                // Try to release back-pressure if below low-water mark
                if (currentSize < bufferCapacity.times(lowWaterMark) && !ctx.channel().config().isAutoRead) {
                    ctx.channel().config().isAutoRead = true
                    ctx.read()
                }
            }
        } finally {
            drainLock.set(false)
            tryDrainBuffer(ctx, websocket) // prevent deadlock by trying to drain immediately after release
        }
    }

    /**
     * Only drain the buffer:
     * 1. If we can get an exclusive lock
     * 2. On the app executor to prevent blocking the netty event loop group
     */
    private fun tryDrainBuffer(ctx: ChannelHandlerContext, websocket: PushPullAdaptingWebSocket) {
        if (messageBuffer.isNotEmpty() && drainLock.compareAndSet(false, true)) {
            appExecutor.execute { drainBuffer(ctx, websocket) }
        }
    }

    private fun closeWebsocket(ctx: ChannelHandlerContext, websocket: PushPullAdaptingWebSocket, msg: CloseWebSocketFrame) {
        ctx.writeAndFlush(msg).addListeners(ChannelFutureListener {
            normalClose = msg.statusCode() == WsStatus.NORMAL.code
            websocket.triggerClose(WsStatus(msg.statusCode(), msg.reasonText()))
        }, CLOSE)
    }
}
