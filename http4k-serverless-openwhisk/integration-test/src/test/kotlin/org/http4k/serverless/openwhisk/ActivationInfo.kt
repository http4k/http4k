package org.http4k.serverless.openwhisk

data class ActivationInfo(
    val id: String?,
    val result: Map<String, Any>?,
    val stdout: String?,
    val stderr: String?
)
