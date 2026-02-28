package org.http4k.wiretap.domain

import org.http4k.core.HttpTransaction
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicLong

interface TransactionStore {
    fun record(transaction: HttpTransaction, direction: Direction): WiretapTransaction
    fun list(
        filter: TransactionFilter = TransactionFilter(),
        limit: Int = 500,
        cursor: Long? = null
    ): List<WiretapTransaction>

    fun get(id: Long): WiretapTransaction?
    fun subscribe(fn: (WiretapTransaction) -> Unit): () -> Unit
    fun clear()

    companion object {
        fun InMemory(maxSize: Int = 500) = object : TransactionStore {
            private val nextId = AtomicLong()
            private val transactions = ConcurrentLinkedDeque<WiretapTransaction>()
            private val subscribers = CopyOnWriteArrayList<(WiretapTransaction) -> Unit>()

            override fun record(transaction: HttpTransaction, direction: Direction): WiretapTransaction {
                val wiretapTransaction = WiretapTransaction(nextId.incrementAndGet(), transaction, direction)
                transactions.addFirst(wiretapTransaction)
                while (transactions.size > maxSize) {
                    transactions.removeLast()
                }
                subscribers.forEach { it(wiretapTransaction) }
                return wiretapTransaction
            }

            override fun list(filter: TransactionFilter, limit: Int, cursor: Long?): List<WiretapTransaction> {
                val base = if (cursor != null) transactions.filter { it.id < cursor } else transactions.toList()
                return base
                    .filter { it.toSummary().matches(filter) }
                    .take(limit)
            }

            override fun get(id: Long): WiretapTransaction? = transactions.find { it.id == id }

            override fun subscribe(fn: (WiretapTransaction) -> Unit): () -> Unit {
                subscribers.add(fn)
                return { subscribers.remove(fn) }
            }

            override fun clear() {
                transactions.clear()
            }
        }

    }
}
