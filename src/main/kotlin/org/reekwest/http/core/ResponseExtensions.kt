package org.reekwest.http.core

import org.reekwest.http.core.Status.Companion.NOT_FOUND
import org.reekwest.http.core.Status.Companion.OK

fun ok(headers: Headers = listOf(), entity: Entity? = null) = Response(OK, headers, entity)

fun notFound(headers: Headers = listOf(), entity: Entity? = null) = Response(NOT_FOUND, headers, entity)

fun Response.entity(entity: Entity): Response = copy(entity = entity)