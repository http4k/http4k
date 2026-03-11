package org.http4k.connect.x402.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class PaymentRequired(
    val x402Version: Int,
    val error: String,
    val accepts: List<PaymentRequirements>,
    val resource: ResourceInfo? = null
)
