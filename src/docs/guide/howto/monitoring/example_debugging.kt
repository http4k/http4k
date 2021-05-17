package guide.howto.monitoring

import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.filter.DebuggingFilters

fun main() {

    val app = { _: Request -> Response(OK).body("hello there you look nice today") }

    val debuggedApp = DebuggingFilters.PrintRequestAndResponse().then(app)

    debuggedApp(Request(GET, "/foobar").header("Accepted", "my-great-content/type"))
}
