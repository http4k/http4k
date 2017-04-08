package org.reekwest.http

import org.reekwest.http.core.Method
import org.reekwest.http.core.Request
import org.reekwest.http.core.Uri
import org.reekwest.http.core.body.toBody
import org.reekwest.http.core.contract.*

fun MessagePart<Request, String?>.toInt() = this.map(Integer::parseInt)

@JvmName("toInts")
fun MessagePart<Request, List<String?>>.toInt() = this.map {it.mapNotNull(Integer::parseInt)}

fun main(args: Array<String>) {
    val request: Request = Request(Method.GET, Uri.uri("/?hello=123&hello"), listOf("hello" to "bob"), "asd=23423&asd=23423".toBody())

    println(request[Query.optional("hello").toInt()])
    println(request[Query.required("hello").toInt()])
    println(request[Query.multi.optional("hello").toInt()])
    println(request[Header.required("hello")])
    println(request[Body.string()])
    println(request[Body.form()])
}