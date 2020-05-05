package org.http4k.openapi.server

import com.squareup.kotlinpoet.FunSpec
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.openapi.OpenApi3Spec
import org.http4k.openapi.PathSpec
import org.http4k.poet.Property
import org.http4k.poet.addCodeBlocks
import org.http4k.poet.lensDeclarations
import org.http4k.poet.packageMember
import org.http4k.routing.RoutingHttpHandler

fun OpenApi3Spec.buildEndpoint(path: String, method: Method, pathSpec: PathSpec): FunSpec {
    val functionName = pathSpec.operationId ?: method.toString().toLowerCase() + path.replace('/', '_')

    return FunSpec.builder(functionName.capitalize())
        .returns(Property<RoutingHttpHandler>().type)
        .addCodeBlocks(lensDeclarations(pathSpec))
        .addStatement("return·\"$path\"·%M·%T.$method·to·{ %T(%T.OK) }",
            packageMember<RoutingHttpHandler>("bind"),
            Property<Method>().type,
            Property<Response>().type,
            Property<Status>().type)
        .build()
}
