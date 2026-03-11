package org.http4k.connect.x402.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class SettleResponse(
    val success: Boolean,
    val errorReason: String? = null,
    val transaction: TransactionHash? = null,
    val network: PaymentNetwork? = null,
    val payer: WalletAddress? = null
)
