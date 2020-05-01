package org.http4k.openapi.client

import com.squareup.kotlinpoet.FunSpec
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.openapi.OpenApi3Spec
import org.http4k.openapi.PathSpec
import org.http4k.poet.Property
import org.http4k.poet.Property.Companion.addReturnType

fun OpenApi3Spec.function(path: String, method: Method, pathSpec: PathSpec): FunSpec {
    val functionName = pathSpec.operationId ?: method.name.toLowerCase() + path.replace('/', '_')

    pathSpec.parameters
    return FunSpec.builder(functionName)
        .addReturnType(Property<Response>())
        .addStatement("return·httpHandler(%T(%T.$method,·\"$path\"))",
            Property<Request>().type,
            Property<Method>().type
        )
        .build()
}
