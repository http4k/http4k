package org.http4k.serverless.testing.client.model

data class ActivationResult(
    val status: String?,
    val result: Map<String, Any>?,
    val success: Boolean?,
    val size: Int?
)
