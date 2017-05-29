package cookbook

import org.http4k.core.Method.DELETE
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.by
import org.http4k.routing.routes

/**
 * This example shows how to use the simple routing functionality to bind several routes
 */
fun main(args: Array<String>) {

    val app = routes(
        GET to "bob" by { Response(OK).body("you GET bob") },
        POST to "rita" by { Response(OK).body("you POST rita") },
        DELETE to "sue" by { Response(OK).body("you DELETE sue") }
    )

    println(app(Request(GET, "/bob")))
    println(app(Request(POST, "/bob")))
    println(app(Request(DELETE, "/sue")))
}
