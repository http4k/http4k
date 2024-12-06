package org.http4k.server

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpServerCodec
import io.netty.handler.codec.http.HttpServerKeepAliveHandler
import io.netty.handler.stream.ChunkedWriteHandler
import org.http4k.core.HttpHandler
import org.http4k.server.ServerConfig.StopMode
import org.http4k.server.ServerConfig.StopMode.Graceful
import org.http4k.server.ServerConfig.StopMode.Immediate
import org.http4k.sse.SseHandler
import org.http4k.websocket.WsHandler
import java.net.InetSocketAddress
import java.time.Duration.ofSeconds
import java.util.concurrent.TimeUnit.MILLISECONDS

/**
 * Stock version of an Netty Server. Not that if you want to configure your own server instance you
 * can duplicate this code and modify it as required. We are purposefully trying to limit options
 * here to keep the API simple for the 99% of use-cases.
 */
class Netty(private val port: Int = 8000, override val stopMode: StopMode) : PolyServerConfig {
    constructor(port: Int = 8000) : this(port, Graceful(ofSeconds(5)))

    init {
        if (stopMode !is Graceful) {
            throw ServerConfig.UnsupportedStopMode(stopMode)
        }
    }

    val shutdownTimeoutMillis = when (stopMode) {
        is Graceful -> stopMode.timeout.toMillis()
        is Immediate -> 0
    }

    override fun toServer(http: HttpHandler?, ws: WsHandler?, sse: SseHandler?): Http4kServer = object : Http4kServer {
        init {
            if (sse != null) throw UnsupportedOperationException("Netty does not support sse")
        }

        private val masterGroup = NioEventLoopGroup()
        private val workerGroup = NioEventLoopGroup()
        private var closeFuture: ChannelFuture? = null
        private lateinit var address: InetSocketAddress

        override fun start(): Http4kServer = apply {
            val bootstrap = ServerBootstrap()
            bootstrap.group(masterGroup, workerGroup)
                .channelFactory { NioServerSocketChannel() }
                .childHandler(object : ChannelInitializer<SocketChannel>() {
                    public override fun initChannel(ch: SocketChannel) {
                        ch.pipeline().addLast("codec", HttpServerCodec())
                        ch.pipeline().addLast("keepAlive", HttpServerKeepAliveHandler())
                        ch.pipeline().addLast("aggregator", HttpObjectAggregator(Int.MAX_VALUE))

                        if (ws != null) ch.pipeline().addLast("websocket", WebSocketServerHandler(ws))

                        ch.pipeline().addLast("streamer", ChunkedWriteHandler())
                        if (http != null) ch.pipeline().addLast("httpHandler", Http4kChannelHandler(http))
                    }
                })
                .option(ChannelOption.SO_BACKLOG, 1000)
                .childOption(ChannelOption.SO_KEEPALIVE, true)

            val channel = bootstrap.bind(port).sync().channel()
            address = channel.localAddress() as InetSocketAddress
            closeFuture = channel.closeFuture()
        }

        override fun stop() = apply {
            closeFuture?.cancel(false)

            val sleepTime = minOf(2000L, shutdownTimeoutMillis)
            workerGroup.shutdownGracefully(sleepTime, shutdownTimeoutMillis, MILLISECONDS).sync()
            masterGroup.shutdownGracefully(sleepTime, shutdownTimeoutMillis, MILLISECONDS).sync()
        }

        override fun port(): Int = if (port > 0) port else address.port
    }
}
