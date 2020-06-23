package org.http4k.serverless.testing.client.model

data class ActionExec(
    val kind: String?,
    val code: String?,
    val image: String?,
    val main: String?,
    val binary: Boolean?,
    val components: List<String>?
)
