package org.http4k.server

import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpServerCodec
import io.netty.handler.codec.http.HttpServerKeepAliveHandler
import io.netty.handler.stream.ChunkedWriteHandler
import org.http4k.core.HttpHandler
import org.http4k.websocket.WsHandler
import java.io.Closeable

class Http4kChannelInitializer(val ws: WsHandler?, val http: HttpHandler?, val maxSize: Int) : ChannelInitializer<SocketChannel>(),
    Closeable {
    private val appExecutor = defaultExecutor()

    public override fun initChannel(ch: SocketChannel) {
        ch.pipeline().addLast("codec", HttpServerCodec())
        ch.pipeline().addLast("keepAlive", HttpServerKeepAliveHandler())
        ch.pipeline().addLast("aggregator", HttpObjectAggregator(maxSize))

        if (ws != null) ch.pipeline().addLast("websocket", WebSocketServerHandler(ws, appExecutor))

        ch.pipeline().addLast("streamer", ChunkedWriteHandler())
        if (http != null) ch.pipeline().addLast("httpHandler", Http4kChannelHandler(http, appExecutor))
    }

    override fun close() {
        appExecutor.shutdown()
    }
}
