package features

import org.http4k.config.Environment
import org.http4k.core.Uri
import org.http4k.fanbelt.bus.memory.InMemoryBroker
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.infrastructure.AppRuntime
import org.http4k.fanbelt.presence.memory.InMemoryPresenceBackend
import org.http4k.fanbelt.store.memory.InMemoryDocumentStorage
import org.http4k.fanbelt.tenancy.TenantId
import org.http4k.server.Jetty
import org.http4k.server.asServer
import org.http4k.server.uri
import java.time.Clock

fun main() {
    val storage = InMemoryDocumentStorage()
    Spaces(storage).create(TenantId.of("demo"), SpaceDetails(Clock.systemUTC().instant()))
    val broker = InMemoryBroker()
    val presence = InMemoryPresenceBackend(Clock.systemUTC())
    val statusService = StandInStatusService().start()

    val nodes = listOf(9001, 9002).map { port ->
        App(AppRuntime(), { AppInfra(it, storage, broker.transport(), presence, Environment.EMPTY) }, statusService = statusService.uri())
            .asServer(Jetty(port)).start()
    }
    val ci = StandInCi(Uri.of("http://localhost:9001/spaces/demo/"))
    Runtime.getRuntime().addShutdownHook(Thread { ci.close(); nodes.forEach { it.stop() }; statusService.stop() })
    nodes.forEach { println("Fanbelt features node on http://localhost:${it.port()}/spaces/demo/") }
}
