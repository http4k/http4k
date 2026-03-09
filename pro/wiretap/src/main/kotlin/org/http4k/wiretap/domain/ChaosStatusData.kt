/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.domain

data class ChaosStatusData(
    val inboundActive: Boolean,
    val inboundDescription: String,
    val outboundActive: Boolean,
    val outboundDescription: String
)
