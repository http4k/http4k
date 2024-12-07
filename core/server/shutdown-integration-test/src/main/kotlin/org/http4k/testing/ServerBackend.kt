package org.http4k.testing

import org.http4k.server.Apache4Server
import org.http4k.server.ApacheServer
import org.http4k.server.Helidon
import org.http4k.server.Jetty
import org.http4k.server.JettyLoom
import org.http4k.server.KtorCIO
import org.http4k.server.KtorNetty
import org.http4k.server.Netty
import org.http4k.server.Ratpack
import org.http4k.server.ServerConfig
import org.http4k.server.ServerConfig.StopMode
import org.http4k.server.SunHttp
import org.http4k.server.SunHttpLoom
import org.http4k.server.Undertow

enum class ServerBackend : (StopMode) -> ServerConfig {
    Apache {
        override fun invoke(mode: StopMode) = ApacheServer(8000, stopMode = mode)
    },
    Apache4 {
        override fun invoke(mode: StopMode) = Apache4Server(8000, stopMode = mode)
    },
    Jetty {
        override fun invoke(mode: StopMode) = Jetty(port = 8000, stopMode = mode)
    },
    JettyLoom {
        override fun invoke(mode: StopMode) = JettyLoom(port = 8000, stopMode = mode)
    },
    Helidon {
        override fun invoke(mode: StopMode) = Helidon(port = 8000, stopMode = mode)
    },
    KtorCIO {
        override fun invoke(mode: StopMode) = KtorCIO(8000, stopMode = mode)
    },
    KtorNetty {
        override fun invoke(mode: StopMode) = KtorNetty(8000, stopMode = mode)
    },
    Netty {
        override fun invoke(mode: StopMode) = Netty(8000, stopMode = mode)
    },
    Ratpack {
        override fun invoke(mode: StopMode) = Ratpack(port = 8000, stopMode = mode)
    },
    SunHttp {
        override fun invoke(mode: StopMode) = SunHttp(port = 8000, stopMode = mode)
    },
    SunHttpLoom {
        override fun invoke(mode: StopMode) = SunHttpLoom(port = 8000, stopMode = mode)
    },
    Undertow {
        override fun invoke(mode: StopMode) = Undertow(port = 8000, mode)
    }
}
