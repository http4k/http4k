package sheets

import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.infrastructure.AppRuntime
import org.http4k.fanbelt.store.memory.InMemoryDocumentStorage
import org.http4k.fanbelt.tenancy.TenantId
import org.http4k.server.Jetty
import sheets.domain.SheetDetails
import sheets.store.Sheets

fun main() {
    val runtime = AppRuntime()
    val storage = InMemoryDocumentStorage()
    Sheets(storage).create(TenantId.of("demo"), SheetDetails(runtime.clock.instant()))
    val server = App(runtime, { AppInfra(it, storage) }).asServer(Jetty(0)).start()
    Runtime.getRuntime().addShutdownHook(Thread(server::stop))
    println("Fanbelt sheets on http://localhost:${server.port()}")
}
