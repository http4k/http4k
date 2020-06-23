package org.http4k.serverless.testing.client.model

data class ActionLimits(
    val timeout: Int?,
    val memory: Int?,
    val logs: Int?,
    val concurrency: Int?
)
