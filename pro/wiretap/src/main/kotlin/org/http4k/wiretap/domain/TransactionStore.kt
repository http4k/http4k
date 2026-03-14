/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.domain

import org.http4k.core.HttpTransaction
import java.time.Clock
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicLong

interface TransactionStore {
    fun record(transaction: HttpTransaction, direction: Direction): TransactionId
    fun list(
        filter: TransactionFilter = TransactionFilter(),
        limit: Int = Int.MAX_VALUE,
        cursor: TransactionId? = null
    ): List<WiretapTransaction>

    fun get(id: TransactionId): WiretapTransaction?
    fun subscribe(fn: (WiretapTransaction) -> Unit): () -> Unit
    fun clear()

    companion object {
        fun InMemory(maxSize: Int = 500, clock: Clock = Clock.systemUTC()) = object : TransactionStore {
            private val nextId = AtomicLong()
            private val transactions = ConcurrentLinkedDeque<WiretapTransaction>()
            private val subscribers = CopyOnWriteArrayList<(WiretapTransaction) -> Unit>()

            override fun record(transaction: HttpTransaction, direction: Direction): TransactionId {
                val wiretapTransaction = WiretapTransaction(TransactionId.of(nextId.incrementAndGet()), transaction, direction)
                transactions.addFirst(wiretapTransaction)
                while (transactions.size > maxSize) {
                    transactions.removeLast()
                }
                subscribers.forEach { it(wiretapTransaction) }
                return wiretapTransaction.id
            }

            override fun list(filter: TransactionFilter, limit: Int, cursor: TransactionId?): List<WiretapTransaction> {
                val base = if (cursor != null) transactions.filter { it.id.value < cursor.value } else transactions.toList()
                return base
                    .filter { it.toSummary(clock).matches(filter) }
                    .take(limit)
            }

            override fun get(id: TransactionId): WiretapTransaction? = transactions.find { it.id == id }

            override fun subscribe(fn: (WiretapTransaction) -> Unit): () -> Unit {
                subscribers.add(fn)
                return { subscribers.remove(fn) }
            }

            override fun clear() = transactions.clear()
        }
    }
}
