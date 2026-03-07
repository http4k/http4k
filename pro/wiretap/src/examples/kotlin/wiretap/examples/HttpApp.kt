package wiretap.examples

import org.http4k.core.ContentType
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.filter.debug
import org.http4k.lens.contentType
import org.http4k.routing.bind
import org.http4k.routing.routes

fun HttpApp() = routes(
    "/" bind {
        Response(OK)
            .contentType(ContentType.APPLICATION_JSON).body("""{"hello":"world"}""")
    }
)
