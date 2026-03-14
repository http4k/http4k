package org.http4k.wiretap.domain

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.HttpTransaction
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant

class InMemoryTransactionStoreEvictionTest {

    @Test
    fun `evicts oldest when maxSize exceeded`() {
        val store = TransactionStore.InMemory(maxSize = 3)

        fun record(): WiretapTransaction {
            val transaction = HttpTransaction(
                Request(GET, "/test"),
                Response(OK),
                Duration.ofMillis(10),
                start = Instant.now()
            )
            val record = store.record(
                transaction,
                Direction.Inbound
            )
            return WiretapTransaction(record, transaction, Direction.Inbound)
        }

        val tx1 = record()
        val tx2 = record()
        val tx3 = record()
        val tx4 = record()

        val all = store.list()
        assertThat(all.size, equalTo(3))
        assertThat(all.any { it.id == tx1 }, equalTo(false))
        assertThat(all, equalTo(listOf(tx4, tx3, tx2)))
    }
}
