/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.domain

import org.http4k.core.HttpTransaction

data class WiretapTransaction(
    val id: TransactionId,
    val transaction: HttpTransaction,
    val direction: Direction
)
