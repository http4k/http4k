package org.http4k.connect.x402.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class PaymentRequirements(
    val scheme: PaymentScheme,
    val network: PaymentNetwork,
    val asset: AssetAddress,
    val amount: PaymentAmount,
    val payTo: WalletAddress,
    val maxTimeoutSeconds: Int,
    val extra: Map<String, String> = emptyMap()
)
