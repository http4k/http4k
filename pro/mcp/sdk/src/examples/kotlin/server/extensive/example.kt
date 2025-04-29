package server.extensive

import org.http4k.server.Helidon
import org.http4k.server.asServer
import server.extensive.Storage.Companion.InMemory

fun main() {
    MyMcpApp(InMemory).asServer(Helidon(9000)).start()
}

