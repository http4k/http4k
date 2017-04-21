package org.reekwest.http.bob

import org.reekwest.http.core.Method.GET
import org.reekwest.http.core.Request
import org.reekwest.http.core.Uri.Companion.uri
import org.reekwest.http.newcontract.Query


fun main(args: Array<String>) {
    val optional = Query.optional("name")
    val required = Query.required("name")
    println(optional(Request(GET, uri("/bob?name=food"))))
    println(optional(Request(GET, uri("/bob"))))
    println(required(Request(GET, uri("/bob"))))
}