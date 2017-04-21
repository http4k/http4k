package org.reekwest.http.bob

import org.reekwest.http.core.Method.GET
import org.reekwest.http.core.Request
import org.reekwest.http.core.Uri.Companion.uri
import org.reekwest.http.newcontract.Header
import org.reekwest.http.newcontract.Query
import org.reekwest.http.newcontract.long


fun main(args: Array<String>) {
    val optional = Query.optional("name")
    val requiredHeader = Header.long().required("name")
    println(optional(Request(GET, uri("/bob?name=food"))))
    println(optional(Request(GET, uri("/bob"))))
    println(requiredHeader(Request(GET, uri("/bob"), listOf("name" to "123", "name" to "1232"))))
}