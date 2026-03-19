/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.connect.x402.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class PaymentRequired(
    val x402Version: Int,
    val error: String,
    val accepts: List<PaymentRequirements>,
    val resource: ResourceInfo? = null
)
