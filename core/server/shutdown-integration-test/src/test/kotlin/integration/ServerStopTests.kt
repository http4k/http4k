package integration

import org.http4k.server.ClientForServerTesting
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

class ApacheServerStopTest : ServerStopContract(Apache, ClientForServerTesting(), {
    enableImmediateStop()
    enableGracefulStop()
})

class Apache4ServerStopTest : ServerStopContract(Apache4, ClientForServerTesting(), {
    enableImmediateStop()
})

class JettyStopTest : ServerStopContract(Jetty, ClientForServerTesting(), {
    enableGracefulStop()
})

class JettyLoomStopTest : ServerStopContract(JettyLoom, ClientForServerTesting(), {
    enableGracefulStop()
})

class KtorCIOStopTest : ServerStopContract(KtorCIO, ClientForServerTesting(), {
    enableImmediateStop()
})

class KtorNettyStopTest : ServerStopContract(KtorNetty, ClientForServerTesting(), {
    enableImmediateStop()
    enableGracefulStop()
})

class NettyStopTest : ServerStopContract(Netty, ClientForServerTesting(), {
    enableGracefulStop()
})

class RatpackStopTest : ServerStopContract(Ratpack, ClientForServerTesting(), {
    enableImmediateStop()
})

class HelidonStopTest : ServerStopContract(Helidon, ClientForServerTesting(), {
    enableImmediateStop()
})

class SunHttpStopTest : ServerStopContract(SunHttp, ClientForServerTesting(), {
    enableImmediateStop()
    enableGracefulStop()
})

class SunHttpLoomStopTest : ServerStopContract(SunHttpLoom, ClientForServerTesting(), {
    enableImmediateStop()
    enableGracefulStop()
})

class UndertowStopTest : ServerStopContract(Undertow, ClientForServerTesting(), {
    enableImmediateStop()
    enableGracefulStop()
})
