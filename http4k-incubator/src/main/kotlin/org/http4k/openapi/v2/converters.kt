package org.http4k.openapi.v2

import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.openapi.SchemaSpec
import org.http4k.openapi.v3.ComponentsV3Spec
import org.http4k.openapi.v3.MessageBodyV3Spec
import org.http4k.openapi.v3.OpenApi3Spec
import org.http4k.openapi.v3.PathV3Spec
import org.http4k.openapi.v3.RequestBodyV3Spec
import org.http4k.openapi.v3.ResponseV3Spec
import org.http4k.openapi.v3.ParameterSpec as ParameterSpecV3

fun OpenApi2Spec.asV3() = OpenApi3Spec(
    info, paths.mapValues { it.value.mapValues { it.value.asV3() } }, ComponentsV3Spec(components)
)

private fun PathV2Spec.asV3(): PathV3Spec {
    val responses = emptyMap<Int, ResponseV3Spec>()

    val requestBody = parameters.filterIsInstance<ParameterSpec.BodySpec>().firstOrNull()?.let {
        RequestBodyV3Spec(mapOf(
            (consumes.firstOrNull() ?: APPLICATION_JSON.value) to MessageBodyV3Spec(it.schema)
        ))
    }


    return PathV3Spec(
        operationId,
        responses,
        requestBody,
        parameters.filterNot { it is ParameterSpec.BodySpec }.mapNotNull { it.asV3() }
    )
}

private fun ParameterSpec.asV3(): ParameterSpecV3? =
    when (this) {
        is ParameterSpec.CookieSpec -> ParameterSpecV3.CookieSpec(name, required, type.asSchema())
        is ParameterSpec.HeaderSpec -> ParameterSpecV3.HeaderSpec(name, required, type.asSchema())
        is ParameterSpec.PathSpec -> ParameterSpecV3.PathSpec(name, required, type.asSchema())
        is ParameterSpec.QuerySpec -> ParameterSpecV3.QuerySpec(name, required, type.asSchema())
        else -> null
    }

private fun String.asSchema(): SchemaSpec = when (this) {
    "string" -> SchemaSpec.StringSpec
    "integer" -> SchemaSpec.IntegerSpec
    "number" -> SchemaSpec.NumberSpec
    "boolean" -> SchemaSpec.BooleanSpec
    else -> throw UnsupportedOperationException("cannot support parameter type of $this")
}
