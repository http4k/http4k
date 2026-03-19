/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.connect.mpp.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class Credential(
    val challenge: Challenge,
    val source: PaymentSource? = null,
    val payload: Map<String, String> = emptyMap()
)
