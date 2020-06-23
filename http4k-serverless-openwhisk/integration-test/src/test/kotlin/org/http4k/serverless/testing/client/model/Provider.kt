package org.http4k.serverless.testing.client.model

data class Provider(
    val name: String,
    val publish: Boolean?,
    val parameters: List<KeyValue>?
)
