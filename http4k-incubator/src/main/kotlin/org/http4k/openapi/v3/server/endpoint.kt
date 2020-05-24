package org.http4k.openapi.v3.server

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.openapi.v3.OpenApi3ParameterSpec
import org.http4k.openapi.v3.Path
import org.http4k.openapi.v3.client.bindFirstToHttpMessage
import org.http4k.poet.Property
import org.http4k.poet.addCodeBlocks
import org.http4k.poet.packageMember
import org.http4k.poet.parameterLensDeclarations
import org.http4k.poet.requestLensDeclarations
import org.http4k.poet.responseLensDeclarations
import org.http4k.poet.supportsFormContent
import org.http4k.poet.webFormLensDeclaration
import org.http4k.routing.RoutingHttpHandler

fun Path.buildEndpoint(modelPackageName: String) = with(this) {
    FunSpec.builder(uniqueName)
        .addKdoc(buildKDoc())
        .returns(Property<RoutingHttpHandler>().type)
        .addCodeBlocks((
            requestLensDeclarations(modelPackageName) +
                responseLensDeclarations(modelPackageName) +
                parameterLensDeclarations() +
                webFormLensDeclaration()
            ).distinct())
        .addCode("\n")
        .addCode("return·\"$urlPathPattern\"·%M·%T.${method}·to·", packageMember<RoutingHttpHandler>("bind"), Property<Method>().type)
        .addCode(buildHandlerCode())
        .build()
}

private fun Path.buildHandlerCode(): CodeBlock {
    val body = CodeBlock.builder()

    if (supportsFormContent()) body.addStatement("val form = formLens(req)")

    (spec.parameters.map { it.name to if (it is OpenApi3ParameterSpec.FormFieldSpec) "form" else "req" } +
        requestSchemas().map { it.fieldName to "req" }
        )
        .forEach { (it, target) -> body.addStatement("val $it = ${it}Lens($target)") }

    body.addStatement("%T(%T.OK)", Property<Response>().type, Property<Status>().type)
    responseSchemas().bindFirstToHttpMessage("TODO()").firstOrNull()?.also { body.add(it) }

    val handler = CodeBlock.builder()
        .addStatement("{ req: %T ->", Property<Request>().type)
        .indent()
        .add(body.build())
        .unindent()
        .add("\n}")
        .build()
    return handler
}
