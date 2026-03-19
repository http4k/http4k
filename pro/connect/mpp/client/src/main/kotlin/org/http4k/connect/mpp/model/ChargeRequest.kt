/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.connect.mpp.model

import org.http4k.connect.mpp.MppMoshi
import se.ansman.kotshi.JsonSerializable
import java.util.Base64

@JsonSerializable
data class ChargeRequest(
    val amount: PaymentAmount,
    val currency: Currency,
    val recipient: Recipient? = null,
    val description: String? = null,
    val externalId: String? = null,
    val methodDetails: Map<String, String>? = null
)

fun ChargeRequest.encodeToRequestParam(): String =
    Base64.getUrlEncoder().withoutPadding().encodeToString(MppMoshi.asFormatString(this).toByteArray())

fun String.decodeToChargeRequest(): ChargeRequest =
    MppMoshi.asA(Base64.getUrlDecoder().decode(this).decodeToString())
