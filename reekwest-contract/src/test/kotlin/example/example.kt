package example

import org.reekwest.http.contract.Path
import org.reekwest.http.contract.Query
import org.reekwest.http.contract.int
import org.reekwest.http.contract.module.ApiKey
import org.reekwest.http.contract.module.Root
import org.reekwest.http.contract.module.Route
import org.reekwest.http.contract.module.RouteModule
import org.reekwest.http.contract.module.SimpleJson
import org.reekwest.http.core.HttpHandler
import org.reekwest.http.core.Method.GET
import org.reekwest.http.core.Request
import org.reekwest.http.core.Response
import org.reekwest.http.core.Status.Companion.OK
import org.reekwest.http.core.Uri.Companion.uri

fun main(args: Array<String>) {

    fun hello(value: String, unused: String, i: Int): HttpHandler = { Response(OK, headers = it.headers) }

    val anInt = Path.int().of("name")

    val asd = Route("")
//        .header(Header.int().required("bob"))
//        .header(Header.int().required("bob2"))
//        .query(Query.optional("goobas"))
        .at(GET) / Path.of("bob") / "hello" / anInt bind ::hello

    val handler = RouteModule(Root / "foo", SimpleJson())
        .securedBy(ApiKey(Query.int().required("api"), { it == 42}))
        .withRoute(asd).toHttpHandler()
    println(handler(Request(GET, uri("/foo/bob/hello/123?api=42"))))
}
