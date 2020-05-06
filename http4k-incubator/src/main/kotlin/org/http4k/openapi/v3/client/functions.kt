package org.http4k.openapi.v3.client

import org.http4k.core.Method
import org.http4k.openapi.v3.OpenApi3Spec

fun OpenApi3Spec.functions() = paths.flatMap { (path, specs) ->
    specs.entries.map { (method, spec) ->
        function(path, Method.valueOf(method.toUpperCase()), spec)
    }
}
