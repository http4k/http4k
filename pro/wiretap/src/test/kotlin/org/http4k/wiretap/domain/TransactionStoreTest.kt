package org.http4k.wiretap.domain

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.HttpTransaction
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.wiretap.domain.Direction.Inbound
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant

class InMemoryTransactionStoreTest : TransactionStoreContract {
    override val store = TransactionStore.InMemory()
}

class InMemoryTransactionStoreEvictionTest {

    @Test
    fun `evicts oldest when maxSize exceeded`() {
        val store = TransactionStore.InMemory(maxSize = 3)

        fun record() = store.record(
            HttpTransaction(Request(GET, "/test"), Response(OK), Duration.ofMillis(10), start = Instant.now()),
            Inbound
        )

        val tx1 = record()
        val tx2 = record()
        val tx3 = record()
        val tx4 = record()

        val all = store.list()
        assertThat(all.size, equalTo(3))
        assertThat(all.contains(tx1), equalTo(false))
        assertThat(all, equalTo(listOf(tx4, tx3, tx2)))
    }
}
