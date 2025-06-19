package integration

import org.http4k.client.JettyClient
import org.http4k.server.ServerConfig
import org.http4k.testing.ServerBackend.Apache
import org.http4k.testing.ServerBackend.Apache4
import org.http4k.testing.ServerBackend.Helidon
import org.http4k.testing.ServerBackend.Jetty
import org.http4k.testing.ServerBackend.JettyLoom
import org.http4k.testing.ServerBackend.KtorCIO
import org.http4k.testing.ServerBackend.KtorNetty
import org.http4k.testing.ServerBackend.Netty
import org.http4k.testing.ServerBackend.Ratpack
import org.http4k.testing.ServerBackend.SunHttp
import org.http4k.testing.ServerBackend.SunHttpLoom
import org.http4k.testing.ServerBackend.Undertow
import org.junit.jupiter.api.Disabled
import java.time.Duration

class ApacheServerStopTest : ServerStopContract(Apache, JettyClient(), {
    enableImmediateStop()
    enableGracefulStop()
})

class Apache4ServerStopTest : ServerStopContract(Apache4, JettyClient(), {
    enableImmediateStop()
})

class JettyStopTest : ServerStopContract(Jetty, JettyClient(), {
    enableGracefulStop()
    enableImmediateStop()
})

@Disabled
class JettyLoomStopTest : ServerStopContract(JettyLoom, JettyClient(), {
    enableGracefulStop()
    enableImmediateStop()
})

@Disabled
class KtorCIOStopTest : ServerStopContract(KtorCIO, JettyClient(), {
    enableImmediateStop()
})

@Disabled
class KtorNettyStopTest : ServerStopContract(KtorNetty, JettyClient(), {
    enableImmediateStop()
    enableGracefulStop()
})

class NettyStopTest : ServerStopContract(Netty, JettyClient(), {
    enableGracefulStop()
})

class RatpackStopTest : ServerStopContract(Ratpack, JettyClient(), {
    enableImmediateStop()
})

class HelidonStopTest : ServerStopContract(Helidon, JettyClient(), {
    enableImmediateStop()
}) {
    @Disabled("Helidon is in a half-broken state with respect to stop modes. Graceful stop is supported, but does not work in docker as expected.")
    override fun `server config throws when invoked with unsupported stop mode`() {
        super.`server config throws when invoked with unsupported stop mode`()
    }
}

class SunHttpStopTest : ServerStopContract(SunHttp, JettyClient(), {
    enableImmediateStop()
    enableGracefulStop()
})

class SunHttpLoomStopTest : ServerStopContract(SunHttpLoom, JettyClient(), {
    enableImmediateStop()
    enableGracefulStop()
})

class UndertowStopTest : ServerStopContract(Undertow, JettyClient(), {
    enableImmediateStop()
    enableGracefulStop()
})
