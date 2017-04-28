package example

import org.reekwest.http.core.HttpHandler
import org.reekwest.http.core.Method.GET
import org.reekwest.http.core.Request
import org.reekwest.http.core.Response
import org.reekwest.http.core.Status.Companion.OK
import org.reekwest.http.core.Uri.Companion.uri
import org.reekwest.kontrakt.Path
import org.reekwest.kontrakt.int
import org.reekwest.kontrakt.module.Root
import org.reekwest.kontrakt.module.Route
import org.reekwest.kontrakt.module.RouteModule
import org.reekwest.kontrakt.module.SimpleJson

fun main(args: Array<String>) {

    fun hello(value: String, unused: String, i: Int): HttpHandler = { Response(OK) }

    val anInt = Path.int().of("name")

    val asd = Route("")
//        .header(Header.int().required("bob"))
//        .header(Header.int().required("bob2"))
//        .query(Query.optional("goobas"))
        .at(GET) / Path.of("bob") / "hello" / anInt bind ::hello

    val handler = RouteModule(Root / "foo", SimpleJson()).withRoute(asd).toHttpHandler()
    println(handler(Request(GET, uri("/foo/bob/hello/123"))))
    println(handler(Request(GET, uri("/"))))
}
