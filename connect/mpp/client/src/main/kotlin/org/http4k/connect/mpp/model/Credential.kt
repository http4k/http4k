package org.http4k.connect.mpp.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class Credential(
    val challenge: Challenge,
    val source: PaymentSource? = null,
    val payload: Map<String, String> = emptyMap()
)
