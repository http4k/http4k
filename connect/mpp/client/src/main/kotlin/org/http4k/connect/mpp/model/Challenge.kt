package org.http4k.connect.mpp.model

import se.ansman.kotshi.JsonSerializable
import java.time.Instant

@JsonSerializable
data class Challenge(
    val id: ChallengeId,
    val realm: Realm,
    val method: PaymentMethod,
    val intent: PaymentIntent,
    val request: ChargeRequest? = null,
    val expires: Instant? = null,
    val description: String? = null,
    val opaque: String? = null
)
