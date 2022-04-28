package integration

import org.http4k.client.ApacheClient
import org.http4k.testing.ServerBackend.Apache
import org.http4k.testing.ServerBackend.Apache4
import org.http4k.testing.ServerBackend.Jetty
import org.http4k.testing.ServerBackend.KtorCIO
import org.http4k.testing.ServerBackend.KtorNetty
import org.http4k.testing.ServerBackend.Netty
import org.http4k.testing.ServerBackend.Ratpack
import org.http4k.testing.ServerBackend.SunHttp
import org.http4k.testing.ServerBackend.Undertow

class ApacheServerStopTest : ServerStopContract(Apache, ApacheClient(), {
    enableImmediateStop()
    enableGracefulStop()
})

class Apache4ServerStopTest : ServerStopContract(Apache4, ApacheClient(), {
    enableImmediateStop()
})

class JettyStopTest : ServerStopContract(Jetty, ApacheClient(), {
    enableGracefulStop()
})

class KtorCIOStopTest : ServerStopContract(KtorCIO, ApacheClient(), {
    enableImmediateStop()
})

class KtorNettyStopTest : ServerStopContract(KtorNetty, ApacheClient(), {
    enableImmediateStop()
})

class NettyStopTest : ServerStopContract(Netty, ApacheClient(), {
    enableGracefulStop()
})

class RatpackStopTest : ServerStopContract(Ratpack, ApacheClient(), {
    enableImmediateStop()
})

class SunHttpStopTest : ServerStopContract(SunHttp, ApacheClient(), {
    enableImmediateStop()
    enableGracefulStop()
})

class UndertowStopTest : ServerStopContract(Undertow, ApacheClient(), {
    enableImmediateStop()
    enableGracefulStop()
})
