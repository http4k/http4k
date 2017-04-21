package org.reekwest.http.newcontract

import org.reekwest.http.contract.*
import org.reekwest.http.contract.Body
import org.reekwest.http.core.*
import org.reekwest.http.core.Method.GET
import org.reekwest.http.core.Uri.Companion.uri
import org.reekwest.http.core.body.toBody


data class CustomType(val s: String)

fun main(args: Array<String>) {
    val optional = Query.optional("name")
    val o2 = Header.map { it + it }.optional("name")
    val requiredHeader = Header.long().multi.required("name")
    val b = Body.string.map(::CustomType).required("description")
    val request = Request(GET, uri("/bob"), listOf(
        "Content-Type" to ContentType.APPLICATION_FORM_URLENCODED.value,
        "name" to "123",
        "name" to "1232"), "hello=body&hello=world".toBody())

    val form = Body.form()

    val formField = FormField.multi.required("hello")
    println(formField(form(request)))
    println(o2(request))
    println(optional(request))
    println(b(request))
//    println(requiredHeader(request))
    println(requiredHeader(listOf(4444L, 4124L), request))
}