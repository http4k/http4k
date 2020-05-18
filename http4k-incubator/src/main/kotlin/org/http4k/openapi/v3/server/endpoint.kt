package org.http4k.openapi.v3.server

import com.squareup.kotlinpoet.FunSpec
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.openapi.v3.PathV3
import org.http4k.poet.Property
import org.http4k.poet.addCodeBlocks
import org.http4k.poet.lensDeclarations
import org.http4k.poet.packageMember
import org.http4k.routing.RoutingHttpHandler

fun PathV3.buildEndpoint(modelPackageName: String) = with(this) {
    FunSpec.builder(uniqueName)
        .returns(Property<RoutingHttpHandler>().type)
        .addCodeBlocks(lensDeclarations(modelPackageName))
        .addStatement("return·\"$urlPathPattern\"·%M·%T.${method}·to·{ %T(%T.OK) }",
            packageMember<RoutingHttpHandler>("bind"),
            Property<Method>().type,
            Property<Response>().type,
            Property<Status>().type)
        .build()
}
