package org.reekwest.http.contract.spike

import org.reekwest.http.contract.Header
import org.reekwest.http.contract.Query
import org.reekwest.http.contract.int
import org.reekwest.http.core.HttpHandler
import org.reekwest.http.core.Method.GET
import org.reekwest.http.core.Request
import org.reekwest.http.core.Response
import org.reekwest.http.core.Status
import org.reekwest.http.core.Uri.Companion.uri

fun main(args: Array<String>) {

    fun hello(value: String, i: Int): HttpHandler = { Response(Status.OK) }

    val anInt = Path.int().of("name")

    val asd = Route("")
        .header(Header.int().required("bob"))
        .header(Header.int().required("bob2"))
        .query(Query.optional("goobas")) / Path.of("bob") / anInt at GET bind ::hello

    val handler = RouteModule(Root).withRoute(asd).toHttpHandler()
    println(handler(Request(GET, uri("/bob/123"))))
}

