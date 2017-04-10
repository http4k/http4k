package org.reekwest.http.core

import org.reekwest.http.core.Status.Companion.NOT_FOUND
import org.reekwest.http.core.Status.Companion.OK
import org.reekwest.http.core.body.Body

fun ok(headers: Headers = listOf(), body: Body? = null) = Response(OK, headers, body)

fun notFound(headers: Headers = listOf(), body: Body? = null) = Response(NOT_FOUND, headers, body)

fun Response.header(name: String, value: String?) = copy(headers = headers.plus(name to value))

fun Response.replaceHeader(name: String, value: String?) = removeHeader(name).header(name, value)

fun Response.removeHeader(name: String) = copy(headers = headers.filterNot { it.first.equals(name, true) })
