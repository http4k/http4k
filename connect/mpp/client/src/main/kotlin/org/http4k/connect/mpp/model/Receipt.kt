package org.http4k.connect.mpp.model

import se.ansman.kotshi.JsonSerializable
import java.time.Instant

@JsonSerializable
data class Receipt(
    val status: ReceiptStatus,
    val method: PaymentMethod,
    val timestamp: Instant,
    val challengeId: ChallengeId,
    val reference: PaymentReference? = null
)
