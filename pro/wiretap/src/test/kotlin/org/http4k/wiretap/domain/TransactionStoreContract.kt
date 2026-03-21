/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.domain

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.HttpTransaction
import org.http4k.core.Method
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.wiretap.domain.Direction.Inbound
import org.http4k.wiretap.domain.Direction.Outbound
import org.http4k.wiretap.domain.Ordering.Descending
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant

interface TransactionStoreContract {

    val store: TransactionStore

    fun record(
        method: Method = GET,
        uri: String = "/test",
        status: Status = OK,
        direction: Direction = Inbound
    ): WiretapTransaction {
        val request = Request(method, uri)
        val response = Response(status)
        val transaction = HttpTransaction(
            request,
            response,
            Duration.ofMillis(10),
            start = Instant.now()
        )
        val id = store.record(transaction, direction)
        return WiretapTransaction(id, transaction,direction)
    }

    @Test
    fun `record returns transaction with unique ids`() {
        val tx1 = record()
        val tx2 = record()

        assertThat(tx1 != tx2, equalTo(true))
        assertThat(store.list(Descending), equalTo(listOf(tx2, tx1)))
    }

    @Test
    fun `list with no filter returns all transactions`() {
        record()
        record()

        assertThat(store.list(Descending).size, equalTo(2))
    }

    @Test
    fun `list filters by direction`() {
        record(direction = Inbound)
        record(direction = Outbound)

        assertThat(
            store.list(Descending, TransactionFilter(direction = Inbound)).size,
            equalTo(1)
        )
    }

    @Test
    fun `list filters by method`() {
        record(method = GET)
        record(method = POST)

        assertThat(
            store.list(Descending, TransactionFilter(method = GET)).size,
            equalTo(1)
        )
    }

    @Test
    fun `list filters by path substring`() {
        record(uri = "/foo/bar")
        record(uri = "/baz")

        assertThat(
            store.list(Descending, TransactionFilter(path = "foo")).size,
            equalTo(1)
        )
    }

    @Test
    fun `list respects limit`() {
        repeat(10) { record() }

        assertThat(
            store.list(Descending, limit = 3).size,
            equalTo(3)
        )
    }

    @Test
    fun `list combines filter and limit`() {
        repeat(5) { record(method = GET) }
        repeat(5) { record(method = POST) }

        assertThat(
            store.list(Descending, TransactionFilter(method = GET), limit = 2).size,
            equalTo(2)
        )
    }

    @Test
    fun `get by id returns recorded transaction`() {
        val tx = record()
        assertThat(store.get(tx.id), equalTo(tx))
    }

    @Test
    fun `get returns null for unknown id`() {
        assertThat(store.get(TransactionId.of(999)), absent())
    }

    @Test
    fun `clear removes all transactions`() {
        record()
        record()
        store.clear()
        assertThat(store.list(Descending).size, equalTo(0))
    }

    @Test
    fun `list filters by status`() {
        record(status = OK)
        record(status = NOT_FOUND)

        assertThat(
            store.list(Descending, TransactionFilter(status = NOT_FOUND)).size,
            equalTo(1)
        )
    }

    @Test
    fun `list filters by host`() {
        record(uri = "http://example.com/test", direction = Outbound)
        record(uri = "http://other.com/test", direction = Outbound)

        assertThat(
            store.list(Descending, TransactionFilter(host = "example")).size,
            equalTo(1)
        )
    }

    @Test
    fun `list with OldestFirst returns chronological order`() {
        val tx1 = record()
        val tx2 = record()

        assertThat(store.list(ordering = Ordering.Ascending, limit = Int.MAX_VALUE), equalTo(listOf(tx1, tx2)))
    }

    @Test
    fun `list with OldestFirst and cursor returns transactions after cursor`() {
        val tx1 = record()
        val tx2 = record()
        val tx3 = record()

        val result = store.list(ordering = Ordering.Ascending, limit = Int.MAX_VALUE, cursor = tx1.id)
        assertThat(result, equalTo(listOf(tx2, tx3)))
    }

    @Test
    fun `list with cursor returns transactions before cursor`() {
        val tx1 = record()
        val tx2 = record()
        val tx3 = record()

        val result = store.list(Descending, cursor = tx3.id)
        assertThat(result, equalTo(listOf(tx2, tx1)))
    }

    @Test
    fun `subscribe notifies on new transactions`() {
        val received = mutableListOf<WiretapTransaction>()
        store.subscribe { received.add(it) }

        val tx = record()
        assertThat(received, equalTo(listOf(tx)))
    }

    @Test
    fun `unsubscribe stops notifications`() {
        val received = mutableListOf<WiretapTransaction>()
        val unsubscribe = store.subscribe { received.add(it) }

        record()
        unsubscribe()
        record()

        assertThat(received.size, equalTo(1))
    }
}
