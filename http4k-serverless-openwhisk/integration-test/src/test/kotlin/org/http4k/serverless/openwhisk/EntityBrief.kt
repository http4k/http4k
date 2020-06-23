package org.http4k.serverless.openwhisk

data class EntityBrief(
    val namespace: String,
    val name: String,
    val version: String,
    val publish: Boolean
)
