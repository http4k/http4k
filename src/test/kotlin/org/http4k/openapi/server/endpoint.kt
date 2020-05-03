package org.http4k.openapi.server

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.asClassName
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.lens.LensSpec
import org.http4k.openapi.OpenApi3Spec
import org.http4k.openapi.PathSpec
import org.http4k.poet.Property
import org.http4k.poet.addCodeBlocks
import org.http4k.poet.lensConstruct
import org.http4k.poet.lensSpecClazz
import org.http4k.poet.packageMember
import org.http4k.poet.quotedName
import org.http4k.routing.RoutingHttpHandler

fun OpenApi3Spec.buildEndpoint(path: String, method: Method, pathSpec: PathSpec): FunSpec {
    val functionName = pathSpec.operationId ?: method.toString().toLowerCase() + path.replace('/', '_')

    val lensDeclarations = pathSpec.parameters.map {
        CodeBlock.of(
            "val ${it.name}Lens = %T.%M().${it.lensConstruct()}(${it.quotedName()})",
            it.lensSpecClazz.asClassName(),
            packageMember<LensSpec<*, *>>(it.schema.clazz!!.simpleName!!.toLowerCase())
        )
    }

    return FunSpec.builder(functionName.capitalize())
        .returns(Property<RoutingHttpHandler>().type)
        .addCodeBlocks(lensDeclarations)
        .addStatement("return·\"$path\"·%M·%T.$method·to·{ %T(%T.OK) }",
            packageMember<RoutingHttpHandler>("bind"),
            Property<Method>().type,
            Property<Response>().type,
            Property<Status>().type)
        .build()
}
