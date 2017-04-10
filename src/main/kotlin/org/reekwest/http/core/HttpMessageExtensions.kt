@file:Suppress("UNCHECKED_CAST")

package org.reekwest.http.core

import sun.plugin.dom.exception.InvalidStateException

fun <T : HttpMessage> T.header(name: String, value: String?) = copy(headers = headers.plus(name to value))

fun <T : HttpMessage> T.replaceHeader(name: String, value: String?) = removeHeader(name).header(name, value)

fun <T : HttpMessage> T.removeHeader(name: String) = copy(headers = headers.filterNot { it.first.equals(name, true) })

fun Request.body(body: Body) = copy(body = body)

fun Response.body(body: Body) = copy(body = body)

private fun <T : HttpMessage> T.copy(headers: Headers = this.headers, body: Body? = this.body): T = when (this) {
    is Request -> this.copy(headers = headers, body = body) as T
    is Response -> this.copy(headers = headers, body = body) as T
    else -> throw InvalidStateException("Unknown class $this")
}
