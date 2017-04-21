package org.reekwest.http.bob

import org.reekwest.http.contract.BiDiMetaLens
import org.reekwest.http.core.Method.GET
import org.reekwest.http.core.Request
import org.reekwest.http.core.Uri.Companion.uri
import org.reekwest.http.newcontract.Header
import org.reekwest.http.newcontract.Query
import org.reekwest.http.newcontract.long


fun main(args: Array<String>) {
    val optional: BiDiMetaLens<Request, String, String?> = Query.optional("name")
    val o2 = Header.map { it + it }.optional("name")
    val multi = Header.long().multi
    val requiredHeader = multi.required("name")
    val request = Request(GET, uri("/bob"), listOf("name" to "123", "name" to "1232"))

    println(o2(request))
    println(optional(request))
//    println(requiredHeader(request))
    println(requiredHeader(listOf(4444L, 4124L), request))
}