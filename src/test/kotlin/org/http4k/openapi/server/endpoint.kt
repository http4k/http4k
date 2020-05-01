package org.http4k.openapi.server

import com.squareup.kotlinpoet.FunSpec
import org.http4k.openapi.OpenApi3Spec
import org.http4k.openapi.PathSpec
import org.http4k.poet.Property
import org.http4k.routing.RoutingHttpHandler

fun OpenApi3Spec.buildEndpoint(it: Map.Entry<String, PathSpec>, path: String): FunSpec {
    val functionName = it.value.operationId ?: it.key + path.replace('/', '_')
    return FunSpec.builder(functionName.capitalize())
        .returns(Property<RoutingHttpHandler>().type)
        .addCode("TODO()")
        .build()
}
