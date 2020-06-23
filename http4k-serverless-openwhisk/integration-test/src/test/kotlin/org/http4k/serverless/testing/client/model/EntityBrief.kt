package org.http4k.serverless.testing.client.model

data class EntityBrief(
    val namespace: String,
    val name: String,
    val version: String,
    val publish: Boolean
)
