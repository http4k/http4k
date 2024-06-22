package guide.howto.deploy_webjars

import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.routing.webJars
import org.http4k.server.SunHttp
import org.http4k.server.asServer

fun main() {
    // mix the WebJars routing into your app...
    val app = routes(
        "/myGreatRoute" bind GET to { _: Request -> Response(OK) },
        webJars()
    )

    app.asServer(SunHttp(8080)).start()

    // then browse to: http://localhost:8080/webjars/swagger-ui/5.1.3/index.html
}
