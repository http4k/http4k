/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.feature

import dev.forkhandles.time.DeterministicScheduler
import org.http4k.fanbelt.feature.Outcome.Denied
import org.http4k.fanbelt.feature.Outcome.Hidden
import org.http4k.fanbelt.feature.Outcome.Rejected
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.infrastructure.AppRuntime
import org.http4k.fanbelt.input.FieldName
import org.http4k.fanbelt.store.Identified
import org.http4k.fanbelt.store.Table
import org.http4k.fanbelt.store.TableName
import org.http4k.fanbelt.store.invoke
import org.http4k.fanbelt.tenancy.TenantId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant

class OutcomeTest {
    private data class Bid(override val id: String, val bidder: String, val amount: Int) : Identified<String>

    private object Auction : Feature<Table<String, Bid>, String>({ tables -> tables(TableName.of("bids")) }) {
        val bid by operation { bids, bidder, amount: Int ->
            val highest = bids.page().maxOfOrNull { it.amount } ?: 100
            bids.insert(Bid("${bids.page().items.size}", bidder, amount))
            if (bidder == "seller") return@operation Denied
            if (bidder == "stranger") return@operation Hidden
            if (amount <= highest) return@operation Rejected(FieldName.of("amount"), "You were outbid: the current bid is $highest")
            Changes.none()
        }
    }

    private val scope = AppInfra(AppRuntime(scheduler = DeterministicScheduler(Instant.EPOCH))).apply { start() }.scope(TenantId.of("acme"))

    private fun bids() = scope.read(Auction) { bids -> bids.page().map { it.amount } }

    @Test
    fun `changes accept the operation`() {
        assertEquals(Changes.none(), scope.execute(Auction.bid, "ann", 110))
        assertEquals(listOf(110), bids())
    }

    @Test
    fun `rejected explains itself on a field, and rolls back what was written before it`() {
        assertEquals(Rejected(mapOf(FieldName.of("amount") to "You were outbid: the current bid is 100")), scope.execute(Auction.bid, "ann", 90))
        assertEquals(emptyList<Int>(), bids())
    }

    @Test
    fun `denied and hidden refuse without a reason, and roll back what was written before them`() {
        assertEquals(Denied, scope.execute(Auction.bid, "seller", 500))
        assertEquals(Hidden, scope.execute(Auction.bid, "stranger", 500))
        assertEquals(emptyList<Int>(), bids())
    }
}
