package org.http4k.openapi.v2

import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.Status
import org.http4k.openapi.MessageBodySpec
import org.http4k.openapi.ResponseSpec
import org.http4k.openapi.SchemaSpec
import org.http4k.openapi.v3.OpenApi3ComponentsSpec
import org.http4k.openapi.v3.OpenApi3PathSpec
import org.http4k.openapi.v3.OpenApi3RequestBodySpec
import org.http4k.openapi.v3.OpenApi3Spec
import org.http4k.openapi.v3.OpenApi3ParameterSpec as ParameterSpecV3

fun OpenApi2Spec.asV3() = OpenApi3Spec(
    info, paths.mapValues { it.value.mapValues { it.value.asV3() } }, OpenApi3ComponentsSpec(definitions)
)

private fun OpenApi2PathSpec.asV3(): OpenApi3PathSpec {
    val requestBody = parameters.filterIsInstance<OpenApi2ParameterSpec.BodySpec>().firstOrNull()?.let {
        OpenApi3RequestBodySpec(it.description, mapOf(
            (consumes.firstOrNull() ?: APPLICATION_JSON.value) to MessageBodySpec(null, it.schema)
        ))
    }

    return OpenApi3PathSpec(
        summary,
        description,
        operationId,
        responses.map {
            (it.key.toIntOrNull() ?: Status.OK.code) to
                ResponseSpec(it.value.description, mapOf((produces.firstOrNull() ?: APPLICATION_JSON.value) to it.value))
        }.toMap(),
        requestBody ?: OpenApi3RequestBodySpec(null),
        parameters.filterNot { it is OpenApi2ParameterSpec.BodySpec }.mapNotNull { it.asV3() }
    )
}

private fun OpenApi2ParameterSpec.asV3() = when (this) {
    is OpenApi2ParameterSpec.CookieSpec -> ParameterSpecV3.CookieSpec(name, required, type.asSchema(this))
    is OpenApi2ParameterSpec.HeaderSpec -> ParameterSpecV3.HeaderSpec(name, required, type.asSchema(this))
    is OpenApi2ParameterSpec.PathSpec -> ParameterSpecV3.PathSpec(name, required, type.asSchema(this))
    is OpenApi2ParameterSpec.QuerySpec -> ParameterSpecV3.QuerySpec(name, required, type.asSchema(this))
    is OpenApi2ParameterSpec.FormSpec -> ParameterSpecV3.FormFieldSpec(name, required, type.asSchema(this))
    else -> null
}

private fun String.asSchema(parameterSpec: OpenApi2ParameterSpec) = when (this) {
    "string" -> SchemaSpec.StringSpec()
    "integer" -> SchemaSpec.IntegerSpec()
    "number" -> SchemaSpec.NumberSpec()
    "boolean" -> SchemaSpec.BooleanSpec()
    "array" -> parameterSpec.itemsSpec()
    else -> throw UnsupportedOperationException("cannot support parameter type of $this")
}

/**
 * For all parameters which are common (and represented in the paths as Refs), inline the content into the path so
 * we can tell the type without looking up from the "global" list
 */
fun OpenApi2Spec.flatten() = copy(paths = paths.mapValues {
        it.value.mapValues {
            val (refs, nonrefs) = it.value.parameters.partition { it is OpenApi2ParameterSpec.RefSpec }
            it.value.copy(parameters = refs.filterIsInstance<OpenApi2ParameterSpec.RefSpec>().map { parameters[it.schemaName]!! } + nonrefs)
        }.toMap()
    })

