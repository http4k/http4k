package org.http4k.server

import io.netty.buffer.Unpooled
import io.netty.buffer.Unpooled.EMPTY_BUFFER
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelFutureListener.CLOSE
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame
import io.netty.handler.codec.http.websocketx.ContinuationWebSocketFrame
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
    private val appExecutor: Executor, // offload message processing to app to avoid blocking the netty event loop
    private val bufferCapacity: Int = 1_000, // beyond this size, apply backpressure to netty
    private val lowWaterMark: Float = 0.5f, // release back-pressure below this percentage of buffer capacity
    private val outgoingFrameSize: Int = 64 * 1024 // 64k; reasonable default
) : SimpleChannelInboundHandler<WebSocketFrame>() {

    private val frameBuffer = ConcurrentLinkedQueue<WebSocketFrame>()
    private val bufferSize = AtomicInteger(0) // because the queue's size getter is O(n)
    private val drainLock = AtomicBoolean()

    @Volatile private var websocket: PushPullAdaptingWebSocket? = null
    @Volatile private var normalClose = false
    @Volatile private var incomingMessage = Unpooled.compositeBuffer() // must be shared because continuations can span across drains
    @Volatile private var previousMessageType: WsMessage.Mode? = null

    override fun handlerAdded(ctx: ChannelHandlerContext) {
        val ws = object : PushPullAdaptingWebSocket() {
            override fun send(message: WsMessage) {
                ctx.writeMessageChunked(message.body.payload.array(), message.mode)
            }

            override fun close(status: WsStatus) {
                ctx.writeAndFlush(CloseWebSocketFrame(status.code, status.description))
                    .addListeners(ChannelFutureListener {
                        normalClose = true
                        websocket?.triggerClose(status)
                    }, CLOSE)
            }
        }

        /* Do not register the websocket until the WsConsumer is invoked.
         * Otherwise, there will be a race between draining messages and registering message handlers.
         */
        appExecutor.execute {
            wSocket(ws)
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

        // Release buffers to prevent memory leak for closed connections
        incomingMessage.release()
        while(frameBuffer.isNotEmpty()) {
            frameBuffer.poll().release()
            bufferSize.decrementAndGet()
        }
    }

    override fun channelRead0(ctx: ChannelHandlerContext, msg: WebSocketFrame) {
        // Always buffer messages to ensure they are processed in the correct order
        frameBuffer.offer(msg.retain())
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

    /**
     * It's very important to guarantee websocket frames are processed in order.  If we submit all frames to the
     * executor independently, we risk out-of-order and/or concurrent processing.  Thus, we buffer incoming frames
     * and only allow a single thread to drain the buffer at a time.
     */
    private fun drainBuffer(ctx: ChannelHandlerContext, websocket: PushPullAdaptingWebSocket) {
        try {
            while (frameBuffer.isNotEmpty()) {
                val msg = frameBuffer.poll() ?: return
                val currentSize = bufferSize.decrementAndGet()

                try {
                    previousMessageType = when(msg) {
                        is TextWebSocketFrame -> WsMessage.Mode.Text
                        is BinaryWebSocketFrame -> WsMessage.Mode.Binary
                        else -> previousMessageType
                    }

                    when (msg) {
                        is TextWebSocketFrame, is BinaryWebSocketFrame, is ContinuationWebSocketFrame -> {
                            incomingMessage.addComponent(true, msg.content().retain())
                            if (msg.isFinalFragment) {
                                flushIncomingMessage(websocket)
                            }
                        }
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
        if (frameBuffer.isNotEmpty() && drainLock.compareAndSet(false, true)) {
            appExecutor.execute { drainBuffer(ctx, websocket) }
        }
    }

    /**
     * Write message in chunks based on the the maximum outgoing frame size.
     * Chunks after the first frame will be sent as continuation frames.
     */
    private fun ChannelHandlerContext.writeMessageChunked(data: ByteArray, mode: WsMessage.Mode) {
        var offset = 0

        while (offset < data.size) {
            val length = minOf(outgoingFrameSize, data.size - offset)
            val isLast = offset + length == data.size
            val buffer = Unpooled.copiedBuffer(data, offset, length)

            val frame = when {
                offset != 0 -> ContinuationWebSocketFrame(isLast, 0, buffer)
                mode == WsMessage.Mode.Text -> TextWebSocketFrame(isLast, 0, buffer)
                else -> BinaryWebSocketFrame(isLast, 0, buffer)
            }

            write(frame)
            offset += length
        }

        flush()
    }

    /**
     * Once a full message has been read, send it to the websocket
     */
    private fun flushIncomingMessage(websocket: PushPullAdaptingWebSocket) {
        // need to copy the data on heap for http4k to consume
        val body = ByteArray(incomingMessage.readableBytes()).also { incomingMessage.readBytes(it) }

        val mode = previousMessageType ?: run {
            websocket.triggerError(IllegalStateException("Received continuation frame without a starting frame"))
            websocket.close(WsStatus.PROTOCOL_ERROR)
            return
        }

        websocket.triggerMessage(WsMessage(body, mode))

        incomingMessage.release()
        incomingMessage = Unpooled.compositeBuffer()
        previousMessageType = null
    }

    private fun closeWebsocket(ctx: ChannelHandlerContext, websocket: PushPullAdaptingWebSocket, msg: CloseWebSocketFrame) {
        ctx.writeAndFlush(msg).addListeners(ChannelFutureListener {
            normalClose = msg.statusCode() == WsStatus.NORMAL.code
            websocket.triggerClose(WsStatus(msg.statusCode(), msg.reasonText()))
        }, CLOSE)
    }
}
