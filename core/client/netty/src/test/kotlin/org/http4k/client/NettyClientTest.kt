package org.http4k.client

import io.netty.bootstrap.Bootstrap
import io.netty.channel.ChannelOption
import io.netty.channel.socket.nio.NioSocketChannel
import org.http4k.core.BodyMode
import org.http4k.server.ApacheServer

class NettyClientTest :
    HttpClientContract(
        ::ApacheServer,
        NettyHttpClient(),
        NettyHttpClient(bootstrap = factoryWithTimeout)
    ), HttpClientWithMemoryModeContract

class NettyClientWithStreamTest :
    HttpClientContract(
        ::ApacheServer,
        NettyHttpClient(),
        NettyHttpClient(bootstrap = factoryWithTimeout, bodyMode = BodyMode.Stream)
    )

private val factoryWithTimeout: BootstrapFactory = { eventLoopGroup ->
    Bootstrap().group(eventLoopGroup)
        .channel(NioSocketChannel::class.java)
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 100)
}
