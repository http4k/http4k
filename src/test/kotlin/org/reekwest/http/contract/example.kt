package org.reekwest.http.contract

import org.reekwest.http.core.HttpHandler
import org.reekwest.http.core.Response
import org.reekwest.http.core.Status
import org.reekwest.http.core.Uri.Companion.uri

fun main(args: Array<String>) {

    fun hello(value: String, i: Int): HttpHandler = { Response(Status.OK) }

    val anInt = Path.int().of("name")

    val asd = Route("")
        .header(Header.int().required("bob"))
        .header(Header.int().required("bob2"))
        .query(Query.optional("goobas")) / Path.of("bob") / anInt at org.reekwest.http.core.Method.GET bind ::hello

    val handler = RouteModule(Root / "foo").withRoute(asd).toHttpHandler()
    println(handler(org.reekwest.http.core.Request(org.reekwest.http.core.Method.GET, uri("/foo/bob/123"))))
}

