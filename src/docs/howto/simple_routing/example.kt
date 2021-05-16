package howto.simple_routing

import org.http4k.core.Method.DELETE
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.bind
import org.http4k.routing.routes

fun main() {

    val app = routes(
        "bob" bind GET to { Response(OK).body("you GET bob") },
        "rita" bind POST to { Response(OK).body("you POST rita") },
        "sue" bind DELETE to { Response(OK).body("you DELETE sue") }
    )

    println(app(Request(GET, "/bob")))
    println(app(Request(POST, "/bob")))
    println(app(Request(DELETE, "/sue")))
}
