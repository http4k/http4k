package org.http4k.openapi.v3.server

import com.squareup.kotlinpoet.FunSpec
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.openapi.v3.OpenApi3Spec
import org.http4k.openapi.v3.Path
import org.http4k.poet.Property
import org.http4k.poet.addCodeBlocks
import org.http4k.poet.lensDeclarations
import org.http4k.poet.packageMember
import org.http4k.routing.RoutingHttpHandler

fun OpenApi3Spec.buildEndpoint(path: Path) = with(path) {
    FunSpec.builder(uniqueName)
        .returns(Property<RoutingHttpHandler>().type)
        .addCodeBlocks(lensDeclarations(this))
        .addStatement("return·\"${urlPathPattern}\"·%M·%T.$method·to·{ %T(%T.OK) }",
            packageMember<RoutingHttpHandler>("bind"),
            Property<Method>().type,
            Property<Response>().type,
            Property<Status>().type)
        .build()
}
