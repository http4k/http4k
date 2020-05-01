package org.http4k.openapi.server

import org.http4k.openapi.OpenApi3Spec

fun OpenApi3Spec.buildEndpoints() = paths
    .flatMap { (path, value) ->
        value.entries.map { buildEndpoint(it, path) }
    }.sortedBy { it.name }
