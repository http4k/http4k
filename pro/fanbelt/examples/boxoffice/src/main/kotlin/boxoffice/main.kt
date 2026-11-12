package boxoffice

import boxoffice.domain.Layout
import boxoffice.domain.ShowTitle
import boxoffice.fakes.FakePaymentProvider
import boxoffice.store.Theatres
import org.http4k.client.JavaHttpClient
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters.SetBaseUriFrom
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.infrastructure.AppRuntime
import org.http4k.fanbelt.store.memory.InMemoryDocumentStorage
import org.http4k.fanbelt.tenancy.TenantId
import org.http4k.server.Jetty
import org.http4k.server.asServer
import java.security.SecureRandom
import java.time.Clock

fun main() {
    val payments = FakePaymentProvider().asServer(Jetty(9200)).start()
    val storage = InMemoryDocumentStorage()
    Theatres(storage).create(TenantId.of("demo"), Clock.systemUTC(), SecureRandom(), ShowTitle.of("Midnight Owls") to Layout(rows = 8, seatsPerRow = 12), ShowTitle.of("The Lantern Choir") to Layout(rows = 5, seatsPerRow = 10))

    val runtime = AppRuntime(http = SetBaseUriFrom(Uri.of("http://localhost:${payments.port()}")).then(JavaHttpClient()))
    val server = App(runtime) { AppInfra(it, storage) }.asServer(Jetty(9000)).start()
    Runtime.getRuntime().addShutdownHook(Thread { server.stop(); payments.stop() })
    println("Fanbelt box office on http://localhost:${server.port()}/theatres/demo/")
}