package org.reekwest.http.contract

import org.reekwest.http.core.Method
import org.reekwest.http.core.Request
import org.reekwest.http.core.Uri
import org.reekwest.http.core.body.toBody
import org.reekwest.http.core.contract.Header

//
//fun Required<Request, String?>.toInt() = this.map(Integer::parseInt)
//fun Optional<Request, String?>.toInt() = this.map(Integer::parseInt)
//
//@JvmName("toInts")
//fun Required<Request, List<String?>>.toInt() = this.map { it.mapNotNull(Integer::parseInt) }
//
//@JvmName("toIntsOpt")
//fun Optional<Request, List<String?>>.toInt() = this.map { it.mapNotNull(Integer::parseInt) }
//
//fun main(args: Array<String>) {
//
//    val a = Header.optional("hello").map { it.length }
//    val message = a[request]
//    println(message)
//
//    println(request[Query.optional("hello").toInt()])
//    println(request[Query.required("hello").toInt()])
//    println(request[Header.required("hello")])
//    println(request[Body.string()])
//    println(request[Body.form()])
//    println(request[Query.multi.optional("hello").toInt()])
//
//}

fun main(args: Array<String>) {
    val request: Request = Request(Method.GET, Uri.uri("/?hello=123&hello"), listOf("hello" to "123"), "asd=23423&asd=23423".toBody())
    println(Header.map { it.reversed() }.optional("hello")(request))
    println(Header.optional("hello")(request))
}

//fun main(args: Array<String>) {
//
//    fun Required<Request, String?>.toInt() = this.map(Integer::parseInt)
//    fun Optional<Request, String?>.toInt() = this.map(Integer::parseInt)
//
//    val request: Request = Request(Method.GET, Uri.uri("/?hello=123&hello"), listOf("hello" to "bob"), "asd=23423&asd=23423".toBody())
//
//    println(request[Query.optional("hello").toInt()])
//    println(request[Query.required("hello").toInt()])
//
//}