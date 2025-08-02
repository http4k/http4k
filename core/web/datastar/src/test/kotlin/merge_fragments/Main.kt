package merge_fragments

import org.http4k.server.JettyLoom
import org.http4k.server.asServer

fun main() {
    UserManagement().asServer(JettyLoom(8999)).start()
}
