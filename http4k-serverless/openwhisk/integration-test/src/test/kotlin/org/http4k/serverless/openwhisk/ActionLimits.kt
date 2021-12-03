package org.http4k.serverless.openwhisk

data class ActionLimits(
    val timeout: Int?,
    val memory: Int?,
    val logs: Int?,
    val concurrency: Int?
)
