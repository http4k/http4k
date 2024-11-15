package org.http4k.serverless.openwhisk

data class ActionExec(
    val kind: String,
    val code: String? = null,
    val image: String? = null,
    val main: String,
    val binary: Boolean? = null,
    val components: List<String>? = null
)
