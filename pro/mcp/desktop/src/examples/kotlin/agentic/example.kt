package agentic

import org.http4k.filter.debug
import org.http4k.server.Helidon
import org.http4k.server.asServer

fun main() {
    al().asServer(Helidon(12000)).start()
    david().asServer(Helidon(13000)).start()
    franck().asServer(Helidon(14000)).start()
    frenchRestaurant().debug(debugStream = true).asServer(Helidon(15000)).start()
}
