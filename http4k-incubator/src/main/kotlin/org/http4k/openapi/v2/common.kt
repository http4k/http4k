package org.http4k.openapi.v2

import org.http4k.openapi.SchemaSpec
import org.http4k.openapi.v3.ComponentsV3Spec
import org.http4k.openapi.v3.OpenApi3Spec
import org.http4k.openapi.v3.PathV3Spec
import org.http4k.openapi.v3.ParameterSpec as ParameterSpecV3

fun PathV2Spec.asV3() = PathV3Spec(operationId, emptyMap(), null, parameters.filter { it.type != "body" }.map { it.asV3() })

private fun ParameterSpec.asV3(): ParameterSpecV3 {
    val schema = when (type) {
        "string" -> SchemaSpec.StringSpec
        "integer" -> SchemaSpec.IntegerSpec
        "number" -> SchemaSpec.NumberSpec
        "boolean" -> SchemaSpec.BooleanSpec
        else -> throw UnsupportedOperationException("cannot support parameter type of $type")
    }
    return when (`in`) {
        "path" -> ParameterSpecV3.PathSpec(name, required, schema)
        "header" -> ParameterSpecV3.HeaderSpec(name, required, schema)
        "query" -> ParameterSpecV3.QuerySpec(name, required, schema)
        "cookie" -> ParameterSpecV3.CookieSpec(name, required, schema)
        else -> throw UnsupportedOperationException("cannot support parameter location of $`in`")
    }
}

fun OpenApi2Spec.asV3() = OpenApi3Spec(
    info, paths.mapValues { it.value.mapValues { it.value.asV3() } }, ComponentsV3Spec(components)
)
