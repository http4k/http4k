package org.reekwest.http.contract

import org.reekwest.http.core.Method
import org.reekwest.http.core.Request
import org.reekwest.http.core.Uri
import org.reekwest.http.core.body.toBody
import org.reekwest.http.core.contract.*

data class MyCustomBodyType(val value: String)

fun Body.toCustom() = Body.string().map(::MyCustomBodyType)

fun Required<Request, String?>.toInt() = this.map(Integer::parseInt)
fun Optional<Request, String?>.toInt() = this.map(Integer::parseInt)

@JvmName("toInts")
fun Required<Request, List<String?>>.toInt() = this.map { it.mapNotNull(Integer::parseInt) }

@JvmName("toIntsOpt")
fun Optional<Request, List<String?>>.toInt() = this.map { it.mapNotNull(Integer::parseInt) }

fun main(args: Array<String>) {
    val request: Request = Request(Method.GET, Uri.Companion.uri("/?hello=123&hello"), listOf("hello" to "bob"), "asd=23423&asd=23423".toBody())

    val a = Header.optional("hello").map { it.length }
    val message = a[request]
    println(message)

    println(request[Query.optional("hello").toInt()])
    println(request[Query.required("hello").toInt()])
    println(request[Header.required("hello")])
    println(request[Body.string()])
    println(request[Body.form()])
    println(request[Body.toCustom()])
    println(request[Query.multi.optional("hello").toInt()])

}
