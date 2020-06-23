package org.http4k.serverless.testing.client.model

data class ActivationBrief(
    val namespace: String,
    val name: String,
    val version: String,
    val publish: Boolean,
    val annotations: List<KeyValue>?,
    val activationId: String,
    val start: Int,
    val end: Int?,
    val duration: Int?,
    val cause: String?,
    val statusCode: Int?
)
