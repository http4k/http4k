package org.http4k.connect.openfga.model

import se.ansman.kotshi.JsonSerializable
import java.time.Instant

@JsonSerializable
data class Store(
    val id: StoreId,
    val name: StoreName,
    val created_at: Instant,
    val updated_at: Instant
)
