package org.http4k.wiretap.domain

import org.http4k.core.HttpTransaction

data class WiretapTransaction(
    val id: Long,
    val transaction: HttpTransaction,
    val direction: Direction
)
