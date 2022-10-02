package blog.nanoservices

import org.http4k.client.JavaHttpClient
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.routing.ResourceLoader.Companion.Directory
import org.http4k.routing.static
import org.http4k.server.Undertow
import org.http4k.server.asServer

fun `static file server`() =
    static(Directory())
        .asServer(Undertow())
        .start()

fun main() {
    `static file server`().use {
        // by default, static servers will only serve known file types, or those registered on construction
        println(
            JavaHttpClient()(
                Request(GET, "http://localhost:8000/version.json")
            )
        )
    }
}
