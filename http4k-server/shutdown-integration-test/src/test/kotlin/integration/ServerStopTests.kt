package integration

import org.http4k.client.ApacheClient
import org.http4k.testing.ServerBackend.Apache
import org.http4k.testing.ServerBackend.Apache4
import org.http4k.testing.ServerBackend.Jetty
import org.http4k.testing.ServerBackend.KtorNetty
import org.http4k.testing.ServerBackend.Netty
import org.http4k.testing.ServerBackend.Ratpack
import org.http4k.testing.ServerBackend.Undertow

class ApacheServerStopTest : ServerStopContract(
    Apache,
    ApacheClient(),
    {
        enableImmediateStop()
        enableGracefulStop()
    }
)

class UndertowStopTest : ServerStopContract(
    Undertow,
    ApacheClient(),
    {
        enableImmediateStop()
        enableGracefulStop()
    })

class RatpackStopTest : ServerStopContract(Ratpack, ApacheClient(), { enableImmediateStop() })

class JettyStopTest : ServerStopContract(
    Jetty,
    ApacheClient(),
    {
        enableGracefulStop()
    }
)

class Apache4ServerStopTest : ServerStopContract(
    Apache4,
    ApacheClient(),
    {
        enableImmediateStop()
    })


class NettyStopTest : ServerStopContract(
    Netty,
    ApacheClient(),
    {
        enableGracefulStop()
    })

class KtorNettyStopTest : ServerStopContract(
    KtorNetty,
    ApacheClient(),
    {
        enableImmediateStop()
    }
)


//class KtorCIOStopTest : ServerStopContract(
//    { stopMode -> KtorCIO(Random().nextInt(1000) + 8745, stopMode) },
//    ApacheClient(),
//    {
//        enableImmediateStop()
//    })

//class SunHttpStopTest : ServerStopContract(
//    { stopMode -> SunHttp(0, stopMode) },
//    ApacheClient(),
//    {
//        enableImmediateStop()
//        enableGracefulStop()
//    }
//)
