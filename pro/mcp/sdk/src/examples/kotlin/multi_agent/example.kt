package multi_agent

import org.http4k.server.Helidon
import org.http4k.server.asServer

fun main() {
    recipePlanner.asServer(Helidon(31000)).start()
    nutritionist.asServer(Helidon(32000)).start()
    shopper.asServer(Helidon(33000)).start()

    UI().recipe()
}
