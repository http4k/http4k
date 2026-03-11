package org.http4k.connect.x402.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class VerifyResponse(
    val isValid: Boolean,
    val invalidReason: String? = null,
    val payer: WalletAddress? = null
)
