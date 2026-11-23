package todo

import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.infrastructure.AppRuntime
import org.http4k.fanbelt.store.memory.InMemoryDocumentStorage
import org.http4k.fanbelt.tenancy.TenantId
import org.http4k.server.Jetty
import todo.domain.TodoList
import todo.store.Lists

fun main() {
    val runtime = AppRuntime()
    val storage = InMemoryDocumentStorage()
    Lists(storage).create(TenantId.of("demo"), TodoList(runtime.clock.instant()))
    val server = App(runtime, { AppInfra(it, storage) }).asServer(Jetty(0)).start()
    Runtime.getRuntime().addShutdownHook(Thread(server::stop))
    println("Fanbelt todos on http://localhost:${server.port()}")
}
