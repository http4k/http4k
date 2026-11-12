package sl4k

import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.store.memory.InMemoryDocumentStorage
import org.http4k.fanbelt.tenancy.TenantId
import org.http4k.server.Jetty
import sl4k.domain.ChannelName
import sl4k.store.Teams
import java.time.Clock

fun main() {
    val storage = InMemoryDocumentStorage()
    Teams(storage).create(TenantId.of("demo"), Clock.systemUTC(), ChannelName.of("general"), ChannelName.of("random"))

    val server = App { AppInfra(it, storage) }.asServer(Jetty(9000)).start()
    Runtime.getRuntime().addShutdownHook(Thread { server.stop() })
    println("sl4k on http://localhost:${server.port()}/teams/demo/")
}
