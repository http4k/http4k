package org.reekwest.http.bob

import org.reekwest.http.contract.BiDiMetaLens
import org.reekwest.http.core.ContentType
import org.reekwest.http.core.Method.GET
import org.reekwest.http.core.Request
import org.reekwest.http.core.Uri.Companion.uri
import org.reekwest.http.core.body.toBody
import org.reekwest.http.newcontract.*


data class CustomType(val s: String)
fun main(args: Array<String>) {
    val optional: BiDiMetaLens<Request, String, String?> = Query.optional("name")
    val o2 = Header.map { it + it }.optional("name")
    val requiredHeader = Header.long().multi.required("name")
    val b = Body.string.map(::CustomType).required("description")
    val request = Request(GET, uri("/bob"), listOf(
        "Content-Type" to ContentType.APPLICATION_FORM_URLENCODED.value,
        "name" to "123",
        "name" to "1232"), "hello=body".toBody())

    val form = Body.form()

    val formField = FormField.required("hello")
    println(formField(form(request)))
    println(o2(request))
    println(optional(request))
    println(b(request))
//    println(requiredHeader(request))
    println(requiredHeader(listOf(4444L, 4124L), request))
}