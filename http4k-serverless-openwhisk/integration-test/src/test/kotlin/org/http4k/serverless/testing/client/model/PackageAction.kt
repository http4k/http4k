package org.http4k.serverless.testing.client.model

data class PackageAction(
    val name: String,
    val version: String,
    val annotations: List<KeyValue>?,
    val parameters: List<KeyValue>?
)
