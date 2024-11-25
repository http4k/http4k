package integration

import org.http4k.client.OkHttp
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

class ApacheServerStopTest : ServerStopContract(Apache, OkHttp(), {
    enableImmediateStop()
    enableGracefulStop()
})

class Apache4ServerStopTest : ServerStopContract(Apache4, OkHttp(), {
    enableImmediateStop()
})

class JettyStopTest : ServerStopContract(Jetty, OkHttp(), {
    enableGracefulStop()
    enableImmediateStop()
})

class JettyLoomStopTest : ServerStopContract(JettyLoom, OkHttp(), {
    enableGracefulStop()
    enableImmediateStop()
})

class KtorCIOStopTest : ServerStopContract(KtorCIO, OkHttp(), {
    enableImmediateStop()
})

class KtorNettyStopTest : ServerStopContract(KtorNetty, OkHttp(), {
    enableImmediateStop()
    enableGracefulStop()
})

class NettyStopTest : ServerStopContract(Netty, OkHttp(), {
    enableGracefulStop()
})

class RatpackStopTest : ServerStopContract(Ratpack, OkHttp(), {
    enableImmediateStop()
})

class HelidonStopTest : ServerStopContract(Helidon, OkHttp(), {
    enableImmediateStop()
})

class SunHttpStopTest : ServerStopContract(SunHttp, OkHttp(), {
    enableImmediateStop()
    enableGracefulStop()
})

class SunHttpLoomStopTest : ServerStopContract(SunHttpLoom, OkHttp(), {
    enableImmediateStop()
    enableGracefulStop()
})

class UndertowStopTest : ServerStopContract(Undertow, OkHttp(), {
    enableImmediateStop()
    enableGracefulStop()
})
