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
    val request = Request(GET, uri("/bob"), listOf("name" to "123", "name" to "1232"))

    println(optional(request))
    println(optional(request))
    println(requiredHeader(request))
    println(requiredHeader(4444, request))
}