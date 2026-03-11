package org.http4k.connect.x402.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class FacilitatorRequest(
    val payload: PaymentPayload,
    val paymentRequirements: PaymentRequirements
)
