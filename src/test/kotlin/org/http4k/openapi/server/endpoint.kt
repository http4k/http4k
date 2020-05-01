package org.http4k.openapi.server

import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.MemberName
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.openapi.OpenApi3Spec
import org.http4k.openapi.PathSpec
import org.http4k.poet.Property
import org.http4k.routing.RoutingHttpHandler

fun OpenApi3Spec.buildEndpoint(method: Method, spec: PathSpec, path: String): FunSpec {
    val functionName = spec.operationId ?: method.toString().toLowerCase() + path.replace('/', '_')

    return FunSpec.builder(functionName.capitalize())
        .returns(Property<RoutingHttpHandler>().type)
        .addStatement("return·\"$path\"·%M·%T.$method·to·{ %T(%T.OK) }",
            MemberName("org.http4k.routing", "bind"),
            Property<Method>().type,
            Property<Response>().type,
            Property<Status>().type)
        .build()
}
