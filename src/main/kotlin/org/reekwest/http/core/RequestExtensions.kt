package org.reekwest.http.core

import org.reekwest.http.core.Method.*
import org.reekwest.http.core.Uri.Companion.uri

fun get(uri: String, headers: Headers = listOf(), body: Body? = null) = Request(GET, uri(uri), headers, body)

fun post(uri: String, headers: Headers = listOf(), body: Body? = null) = Request(POST, uri(uri), headers, body)

fun put(uri: String, headers: Headers = listOf(), body: Body? = null) = Request(PUT, uri(uri), headers, body)

fun Request.query(name: String, value: String) = copy(uri = uri.query(name, value))
