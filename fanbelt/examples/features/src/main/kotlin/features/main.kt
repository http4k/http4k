package features

import org.http4k.core.Uri
import org.http4k.server.asServer
import org.http4k.server.uri
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.infrastructure.AppRuntime
import org.http4k.fanbelt.store.memory.InMemoryDocumentStorage
import org.http4k.fanbelt.tenancy.TenantId
import org.http4k.server.Jetty

fun main() {
    val runtime = AppRuntime()
    val storage = InMemoryDocumentStorage()
    Spaces(storage).create(TenantId.of("demo"), SpaceDetails(runtime.clock.instant()))
    val statusService = StandInStatusService().start()
    val server = App(runtime, { AppInfra(it, storage) }, statusService = statusService.uri()).asServer(Jetty(0)).start()
    StandInCi(Uri.of("http://localhost:${server.port()}/spaces/demo/"))
    println("Fanbelt features on http://localhost:${server.port()}")
}
