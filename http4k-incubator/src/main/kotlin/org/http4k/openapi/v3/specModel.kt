package org.http4k.openapi.v3

import org.http4k.openapi.InfoSpec
import org.http4k.openapi.ParameterSpec
import org.http4k.openapi.SchemaSpec

data class RequestBodyV3Spec(val content: Map<String, MessageBodyV3Spec> = emptyMap())

data class MessageBodyV3Spec(val schema: SchemaSpec?)

data class ComponentsV3Spec(val schemas: Map<String, SchemaSpec> = emptyMap())

data class ResponseV3Spec(val content: Map<String, MessageBodyV3Spec>)

data class PathV3Spec(
    val operationId: String?,
    val summary: String?,
    val description: String?,
    val tags: List<String> = emptyList(),
    val responses: Map<Int, ResponseV3Spec> = emptyMap(),
    val requestBody: RequestBodyV3Spec?,
    val parameters: List<ParameterSpec> = emptyList()
)

data class OpenApi3Spec(val info: InfoSpec, val paths: Map<String, Map<String, PathV3Spec>>, val components: ComponentsV3Spec = ComponentsV3Spec())
