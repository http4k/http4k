package org.http4k.connect.amazon.apigatewayv2.model

import org.http4k.core.Uri

data class ApiName(val value: String)

data class ApiId(val value: String)

data class StageName(val value: String)

data class Stage(val stageName: StageName, val autoDeploy: Boolean) {
    companion object {
        val restDefault = Stage(StageName("default"), true)
        val default = Stage(StageName("\$default"), true)
    }
}

data class ApiDetails(val name: ApiName, val apiId: ApiId, val apiEndpoint: Uri)

data class Integration(
    val integrationType: String = "AWS_PROXY",
    val integrationUri: String,
    val timeoutInMillis: Long = 30000,
    val payloadFormatVersion: String = "1.0"
)

data class IntegrationId(val value: String)

data class IntegrationInfo(val integrationId: IntegrationId)

enum class ApiIntegrationVersion { v1, v2 }
