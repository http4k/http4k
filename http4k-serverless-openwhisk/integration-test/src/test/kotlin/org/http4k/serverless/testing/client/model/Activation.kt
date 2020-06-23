package org.http4k.serverless.testing.client.model

data class Activation(
    val namespace: String,
    val name: String,
    val version: String,
    val publish: Boolean,
    val annotations: List<KeyValue>?,
    val subject: String,
    val activationId: String,
    val start: Int,
    val end: Int?,
    val duration: Int?,
    val response: ActivationResult,
    val logs: List<String>,
    val cause: String?,
    val statusCode: Int?
)
