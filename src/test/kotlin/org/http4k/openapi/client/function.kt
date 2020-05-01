package org.http4k.openapi.client

import com.squareup.kotlinpoet.FunSpec
import org.http4k.core.Method
import org.http4k.openapi.OpenApi3Spec
import org.http4k.openapi.PathSpec

fun OpenApi3Spec.function(path: String, method: Method, pathSpec: PathSpec): FunSpec {
    val functionName = pathSpec.operationId ?: method.name.toLowerCase() + path.replace('/', '_')
    return FunSpec.builder(functionName).build()
}
