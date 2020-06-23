package org.http4k.serverless.openwhisk

data class ErrorMessage(
    val error: String,
    val code: String?
)
