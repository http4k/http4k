package org.http4k.serverless.openwhisk

data class ActivationResult(
    val status: String?,
    val result: Map<String, Any>?,
    val success: Boolean?,
    val size: Int?
)
