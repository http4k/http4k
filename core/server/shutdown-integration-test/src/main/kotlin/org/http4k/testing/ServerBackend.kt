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
        override fun invoke(mode: StopMode) = ApacheServer(PORT, stopMode = mode)
    },
    Apache4 {
        override fun invoke(mode: StopMode) = Apache4Server(PORT, stopMode = mode)
    },
    Jetty {
        override fun invoke(mode: StopMode) = Jetty(port = PORT, stopMode = mode)
    },
    JettyLoom {
        override fun invoke(mode: StopMode) = JettyLoom(port = PORT, stopMode = mode)
    },
    Helidon {
        override fun invoke(mode: StopMode) = Helidon(port = PORT, stopMode = mode)
    },
    KtorCIO {
        override fun invoke(mode: StopMode) = KtorCIO(PORT, stopMode = mode)
    },
    KtorNetty {
        override fun invoke(mode: StopMode) = KtorNetty(PORT, stopMode = mode)
    },
    Netty {
        override fun invoke(mode: StopMode) = Netty(PORT, stopMode = mode)
    },
    Ratpack {
        override fun invoke(mode: StopMode) = Ratpack(PORT, stopMode = mode)
    },
    SunHttp {
        override fun invoke(mode: StopMode) = SunHttp(PORT, stopMode = mode)
    },
    SunHttpLoom {
        override fun invoke(mode: StopMode) = SunHttpLoom(PORT, stopMode = mode)
    },
    Undertow {
        override fun invoke(mode: StopMode) = Undertow(PORT, mode)
    };

    companion object {
        const val PORT = 8000
    }
}
