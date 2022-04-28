package integration

import org.http4k.client.ApacheClient
import org.http4k.testing.ServerBackend

class ApacheServerStopTest : ServerStopContract(
    ServerBackend.Apache,
    ApacheClient(),
    {
        enableImmediateStop()
        enableGracefulStop()
    }
)

class UndertowStopTest : ServerStopContract(
    ServerBackend.Undertow,
    ApacheClient(),
    {
        enableImmediateStop()
        enableGracefulStop()
    })

class RatpackStopTest : ServerStopContract(ServerBackend.Ratpack, ApacheClient(), { enableImmediateStop() })

//class JettyStopTest : org.http4k.server.ServerStopContract(
//    { stopMode -> Jetty(0, stopMode) },
//    ApacheClient(),
//    {
//        enableImmediateStop()
//        enableGracefulStop()
//    }
//)

//class UndertowStopTest : org.http4k.server.ServerStopContract(
//    { stopMode -> Undertow(0, false, stopMode) },
//    ApacheClient(),
//    {
//        enableImmediateStop()
//        enableGracefulStop()
//    })

//class Apache4ServerStopTest : org.http4k.server.ServerStopContract(
//    { stopMode -> Apache4Server(0, stopMode) },
//    ApacheClient(),
//    {
//        enableImmediateStop()
//    })


//class NettyStopTest : org.http4k.server.ServerStopContract(
//    { stopMode -> Netty(0, stopMode) },
//    ApacheClient(),
//    {
//        enableGracefulStop()
//    })

//class KtorNettyStopTest : org.http4k.server.ServerStopContract(
//    { stopMode -> KtorNetty(Random().nextInt(1000) + 7456, stopMode) },
//    ApacheClient(),
//    {
//        enableImmediateStop()
//    }
//)


//class KtorCIOStopTest : org.http4k.server.ServerStopContract(
//    { stopMode -> KtorCIO(Random().nextInt(1000) + 8745, stopMode) },
//    ApacheClient(),
//    {
//        enableImmediateStop()
//    })

//class SunHttpStopTest : org.http4k.server.ServerStopContract(
//    { stopMode -> SunHttp(0, stopMode) },
//    ApacheClient(),
//    {
//        enableImmediateStop()
//        enableGracefulStop()
//    }
//)
