package org.http4k.openapi.v2

import org.http4k.openapi.InfoSpec
import org.http4k.openapi.ParameterSpec
import org.http4k.openapi.SchemaSpec

data class RequestBodyV2Spec(val content: Map<String, MessageBodyV2Spec> = emptyMap())

data class MessageBodyV2Spec(val schema: SchemaSpec?)

data class ComponentsV2Spec(val schemas: Map<String, SchemaSpec> = emptyMap())

data class ResponseV2Spec(val content: Map<String, MessageBodyV2Spec>)

data class PathV2Spec(
    val operationId: String?,
    val summary: String?,
    val description: String?,
    val tags: List<String> = emptyList(),
    val responses: Map<Int, ResponseV2Spec> = emptyMap(),
    val requestBody: RequestBodyV2Spec?,
    val parameters: List<ParameterSpec> = emptyList()
)

data class OpenApi2Spec(val info: InfoSpec, val paths: Map<String, Map<String, PathV2Spec>>, val components: ComponentsV2Spec = ComponentsV2Spec())
