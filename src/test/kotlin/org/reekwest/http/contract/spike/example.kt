package org.reekwest.http.contract.spike

import org.reekwest.http.contract.Query
import org.reekwest.http.core.HttpHandler
import org.reekwest.http.core.Method.GET

fun main(args: Array<String>) {

    fun hello(value: String, i: Int): HttpHandler = TODO()

    val anInt = Path.int().of("name")

    Route("")
        .taking(Query.required("goobas")) / Path.of("bob") / anInt at GET bind ::hello
}

