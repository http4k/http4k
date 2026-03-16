package org.http4k.connect.x402.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class SupportedKind(
    val scheme: PaymentScheme,
    val networks: List<PaymentNetwork>
)
