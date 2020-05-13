package org.http4k.openapi.v3

import com.squareup.kotlinpoet.FileSpec
import org.http4k.core.Method
import java.io.File

data class GenerationOptions(private val basePackage: String, val destinationFolder: File) {
    fun packageName(name: String) = "$basePackage.$name"
}

interface ApiGenerator : (OpenApi3Spec, GenerationOptions) -> List<FileSpec>

/**
 * Convenience type for working with generated code
 */
data class Path(val urlPathPattern: String, val method: Method, val pathSpec: PathSpec) {
    val uniqueName = (pathSpec.operationId ?: method.toString().toLowerCase() + urlPathPattern.replace('/', '_')).capitalize()
}

fun OpenApi3Spec.flattenedPaths() = paths.entries.flatMap { (path, verbs) ->verbs.map { Path(path, Method.valueOf(it.key.toUpperCase()), it.value) } }

fun OpenApi3Spec.apiName() = info.title.capitalize()
