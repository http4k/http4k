package org.http4k.contract.openapi.v3

import org.http4k.contract.openapi.ApiInfo
import org.http4k.contract.openapi.ApiRenderer
import org.http4k.contract.openapi.OpenAPIJackson
import org.http4k.contract.openapi.OpenApiExtension
import org.http4k.contract.openapi.OpenApiVersion
import org.http4k.format.ConfigurableJackson
import org.http4k.contract.jsonschema.v3.FieldRetrieval
import org.http4k.contract.jsonschema.v3.JacksonFieldMetadataRetrievalStrategy
import org.http4k.contract.jsonschema.v3.JacksonJsonNamingAnnotated
import org.http4k.contract.jsonschema.v3.JacksonJsonPropertyAnnotated
import org.http4k.contract.jsonschema.v3.PrimitivesFieldMetadataRetrievalStrategy
import org.http4k.contract.jsonschema.v3.SimpleLookup
import org.http4k.contract.jsonschema.v3.then

/**
 * Defaults for configuring OpenApi3 with Jackson
 */
fun OpenApi3(
    apiInfo: ApiInfo,
    json: ConfigurableJackson = OpenAPIJackson,
    extensions: List<OpenApiExtension> = emptyList(),
    servers: List<ApiServer> = emptyList(),
    version: OpenApiVersion = OpenApiVersion._3_0_0
) =
    OpenApi3(apiInfo, json, extensions, ApiRenderer.Auto(json, AutoJsonToJsonSchema(json)), servers = servers, version = version)

fun AutoJsonToJsonSchema(json: ConfigurableJackson) = org.http4k.contract.jsonschema.v3.AutoJsonToJsonSchema(
    json,
    FieldRetrieval.compose(
        SimpleLookup(
            metadataRetrievalStrategy =
            JacksonFieldMetadataRetrievalStrategy.then(PrimitivesFieldMetadataRetrievalStrategy)
        ),
        FieldRetrieval.compose(JacksonJsonPropertyAnnotated, JacksonJsonNamingAnnotated(json))
    )
)
