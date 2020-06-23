package org.http4k.serverless.testing.client.model

data class ErrorMessage(
    val error: String,
    val code: String?
)
