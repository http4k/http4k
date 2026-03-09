/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.domain

import org.http4k.core.Method
import org.http4k.core.Status

data class TransactionFilter(
    val direction: Direction? = null,
    val host: String? = null,
    val method: Method? = null,
    val status: Status? = null,
    val path: String? = null
)
