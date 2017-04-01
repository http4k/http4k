package org.reekwest.http.core

import org.reekwest.http.core.Method.GET
import org.reekwest.http.core.Method.POST
import org.reekwest.http.core.Method.PUT
import org.reekwest.http.core.Uri.Companion.uri

fun get(uri: String, headers: Headers = listOf(), entity: Entity? = null) = Request(GET, uri(uri), headers, entity)

fun post(uri: String, headers: Headers = listOf(), entity: Entity? = null) = Request(POST, uri(uri), headers, entity)

fun put(uri: String, headers: Headers = listOf(), entity: Entity? = null) = Request(PUT, uri(uri), headers, entity)

fun Request.query(name: String, value: String) = copy(uri = uri.query(name, value))

