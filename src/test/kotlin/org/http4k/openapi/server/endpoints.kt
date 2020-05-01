package org.http4k.openapi.server

import org.http4k.core.Method
import org.http4k.openapi.OpenApi3Spec

fun OpenApi3Spec.buildEndpoints() = paths.flatMap { (path, specs) ->
    specs.entries.map { (method, spec) ->
        buildEndpoint(path, Method.valueOf(method.toUpperCase()), spec)
    }
}.sortedBy { it.name }
