package wiretap.examples

import org.http4k.core.ContentType
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.lens.contentType

fun HttpApp() = { request: Request ->
    Response(OK).headers(request.headers)
        .contentType(ContentType.APPLICATION_JSON).body("""{"hello":"world"}""")
}
