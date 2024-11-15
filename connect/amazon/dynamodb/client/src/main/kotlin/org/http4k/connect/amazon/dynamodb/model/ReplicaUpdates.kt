package org.http4k.connect.amazon.dynamodb.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class ReplicaUpdates(
    val Create: ReplicaCreate? = null,
    val Delete: ReplicaDelete? = null,
    val Update: ReplicaUpdate? = null
)
