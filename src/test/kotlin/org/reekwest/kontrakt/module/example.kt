package org.reekwest.kontrakt.module

import org.reekwest.http.core.HttpHandler
import org.reekwest.http.core.Method.GET
import org.reekwest.http.core.Request
import org.reekwest.http.core.Response
import org.reekwest.http.core.Status.Companion.OK
import org.reekwest.http.core.Uri.Companion.uri
import org.reekwest.kontrakt.Header
import org.reekwest.kontrakt.Path
import org.reekwest.kontrakt.Query
import org.reekwest.kontrakt.int

fun main(args: Array<String>) {

    fun hello(value: String, i: Int): HttpHandler = { Response(OK) }

    val anInt = Path.int().of("name")

    val asd = Route("")
        .header(Header.int().required("bob"))
        .header(Header.int().required("bob2"))
        .query(Query.optional("goobas")) / Path.of("bob") / anInt at GET bind ::hello

    val handler = RouteModule(Root / "foo").withRoute(asd).toHttpHandler()
    println(handler(Request(GET, uri("/foo/bob/123"))))
}

