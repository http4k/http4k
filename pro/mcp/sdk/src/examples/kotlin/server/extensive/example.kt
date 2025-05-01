package server.extensive

import org.http4k.server.JettyLoom
import org.http4k.server.asServer
import server.extensive.Storage.Companion.InMemory

fun main() {
    MyMcpApp(InMemory).asServer(JettyLoom(9000)).start()
}

