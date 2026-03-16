package org.http4k.connect.x402.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class PaymentPayload(
    val x402Version: Int,
    val scheme: PaymentScheme,
    val network: PaymentNetwork,
    val payload: Map<String, String>,
    val resource: String,
    val description: String
)
