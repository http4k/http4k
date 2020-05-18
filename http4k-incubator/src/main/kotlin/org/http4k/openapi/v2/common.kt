package org.http4k.openapi.v2

import org.http4k.core.Method
import org.http4k.openapi.NamedSchema

data class PathV2(val urlPathPattern: String, val method: Method, val pathV2Spec: PathV2Spec) {

    fun requestSchemas(): List<NamedSchema> = emptyList()

    fun responseSchemas(): List<NamedSchema> = emptyList()

    fun allSchemas() = requestSchemas() + responseSchemas()
}

fun OpenApi2Spec.flattenedPaths() = paths.entries.flatMap { (path, verbs) -> verbs.map { PathV2(path, Method.valueOf(it.key.toUpperCase()), it.value) } }
