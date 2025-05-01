package agentic

import org.http4k.server.JettyLoom
import org.http4k.server.asServer

fun main() {
    al().asServer(JettyLoom(12000)).start()
    david().asServer(JettyLoom(13000)).start()
    franck().asServer(JettyLoom(14000)).start()
    frenchRestaurant().asServer(JettyLoom(15000)).start()
}
