package org.http4k.wiretap.domain

import org.http4k.core.HttpTransaction

typealias WiretapTransactionId = Long

data class WiretapTransaction(
    val id: WiretapTransactionId,
    val transaction: HttpTransaction,
    val direction: Direction
)
