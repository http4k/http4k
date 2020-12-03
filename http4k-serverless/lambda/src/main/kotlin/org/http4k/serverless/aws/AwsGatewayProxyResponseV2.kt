package org.http4k.serverless.aws

data class AwsGatewayProxyResponseV2(
    val statusCode: Int = 0,
    val headers: Map<String, String?> = mapOf(),
    val multiValueHeaders: Map<String, List<String?>> = mapOf(),
    val cookies: List<String> = listOf(),
    val body: String? = null,
    val isBase64Encoded: Boolean = false
)
