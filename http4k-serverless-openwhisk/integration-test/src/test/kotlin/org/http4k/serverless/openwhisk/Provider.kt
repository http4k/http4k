package org.http4k.serverless.openwhisk

data class Provider(
    val name: String,
    val publish: Boolean?,
    val parameters: List<KeyValue>?
)
