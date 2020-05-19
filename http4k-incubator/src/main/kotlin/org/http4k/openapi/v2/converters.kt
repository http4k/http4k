package org.http4k.openapi.v2

import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.openapi.MessageBodySpec
import org.http4k.openapi.ResponseSpec
import org.http4k.openapi.SchemaSpec
import org.http4k.openapi.v3.OpenApi3ComponentsSpec
import org.http4k.openapi.v3.OpenApi3Spec
import org.http4k.openapi.v3.OpenApi3PathSpec
import org.http4k.openapi.v3.OpenApi3RequestBodySpec
import org.http4k.openapi.v3.OpenApi3ParameterSpec as ParameterSpecV3

fun OpenApi2Spec.asV3() = OpenApi3Spec(
    info, paths.mapValues { it.value.mapValues { it.value.asV3() } }, OpenApi3ComponentsSpec(components)
)

private fun OpenApi2PathSpec.asV3(): OpenApi3PathSpec {

    val requestBody = parameters.filterIsInstance<OpenApi2ParameterSpec.BodySpec>().firstOrNull()?.let {
        OpenApi3RequestBodySpec(mapOf(
            (consumes.firstOrNull() ?: APPLICATION_JSON.value) to MessageBodySpec(it.schema)
        ))
    }

    return OpenApi3PathSpec(
        operationId,
        responses.map {
            it.key.toInt() to
                ResponseSpec(mapOf((produces.firstOrNull() ?: APPLICATION_JSON.value) to it.value))
        }.toMap(),
        requestBody,
        parameters.filterNot { it is OpenApi2ParameterSpec.BodySpec }.mapNotNull { it.asV3() }
    )
}

private fun OpenApi2ParameterSpec.asV3(): ParameterSpecV3? =
    when (this) {
        is OpenApi2ParameterSpec.CookieSpec -> ParameterSpecV3.CookieSpec(name, required, type.asSchema())
        is OpenApi2ParameterSpec.HeaderSpec -> ParameterSpecV3.HeaderSpec(name, required, type.asSchema())
        is OpenApi2ParameterSpec.PathSpec -> ParameterSpecV3.PathSpec(name, required, type.asSchema())
        is OpenApi2ParameterSpec.QuerySpec -> ParameterSpecV3.QuerySpec(name, required, type.asSchema())
        else -> null
    }

private fun String.asSchema(): SchemaSpec = when (this) {
    "string" -> SchemaSpec.StringSpec
    "integer" -> SchemaSpec.IntegerSpec
    "number" -> SchemaSpec.NumberSpec
    "boolean" -> SchemaSpec.BooleanSpec
    else -> throw UnsupportedOperationException("cannot support parameter type of $this")
}
