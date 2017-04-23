package org.reekwest.http.contract.spike

import org.reekwest.http.contract.Query

fun main(args: Array<String>) {
    Route("")
        .taking(Query.required("goobas"))
    val a = Path.int().of("name")
    a("123")
}

