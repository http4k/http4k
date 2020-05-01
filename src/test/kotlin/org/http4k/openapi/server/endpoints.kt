package org.http4k.openapi.server

import org.http4k.core.Method
import org.http4k.openapi.OpenApi3Spec

fun OpenApi3Spec.buildEndpoints() = paths
    .flatMap { (path, value) ->
        value.entries.map { buildEndpoint(Method.valueOf(it.key.toUpperCase()), it.value, path) }
    }.sortedBy { it.name }
