package org.http4k.openapi.v3.server

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.openapi.v3.PathV3
import org.http4k.openapi.v3.client.bindFirstToHttpMessage
import org.http4k.poet.Property
import org.http4k.poet.addCodeBlocks
import org.http4k.poet.packageMember
import org.http4k.poet.parameterLensDeclarations
import org.http4k.poet.requestLensDeclarations
import org.http4k.poet.responseLensDeclarations
import org.http4k.routing.RoutingHttpHandler

fun PathV3.buildEndpoint(modelPackageName: String) = with(this) {

    val body = CodeBlock.builder()

    requestSchemas()
        .forEach {
            body.addStatement("val ${it.fieldName} = ${it.fieldName}Lens(req)")
        }

    body.add("%T(%T.OK)", Property<Response>().type, Property<Status>().type)
    responseSchemas().bindFirstToHttpMessage("TODO()").firstOrNull()?.also { body.add(it) }

    val handler = CodeBlock.builder()
        .addStatement("{ req: %T ->", Property<Request>().type)
        .indent()
        .add(body.build())
        .unindent()
        .add("\n}")
        .build()

    FunSpec.builder(uniqueName)
        .returns(Property<RoutingHttpHandler>().type)
        .addCodeBlocks(requestLensDeclarations(modelPackageName) + responseLensDeclarations(modelPackageName) + parameterLensDeclarations())
        .addCode("\n")
        .addCode("return·\"$urlPathPattern\"·%M·%T.${method}·to·", packageMember<RoutingHttpHandler>("bind"), Property<Method>().type)
        .addCode(handler)
        .build()
}
