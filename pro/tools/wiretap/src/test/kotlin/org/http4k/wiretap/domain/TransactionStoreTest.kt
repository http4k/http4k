package org.http4k.wiretap.domain

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
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant

class TransactionStoreTest {

    private val store = TransactionStore.InMemory()

    private fun record(
        method: Method = GET,
        uri: String = "/test",
        status: Status = OK,
        direction: Direction = Inbound
    ) = store.record(
        HttpTransaction(
            Request(method, uri),
            Response(status),
            Duration.ofMillis(10),
            start = Instant.now()
        ),
        direction
    )

    @Test
    fun `record returns transaction with unique ids`() {
        val tx1 = record()
        val tx2 = record()

        assertThat(tx1.id != tx2.id, equalTo(true))
        assertThat(tx1.direction, equalTo(Inbound))
        assertThat(store.list(), equalTo(listOf(tx2, tx1)))
    }

    @Test
    fun `list with no filter returns all transactions`() {
        record()
        record()

        assertThat(store.list().size, equalTo(2))
    }

    @Test
    fun `list filters by direction`() {
        record(direction = Inbound)
        record(direction = Outbound)

        assertThat(
            store.list(TransactionFilter(direction = Inbound)).size,
            equalTo(1)
        )
    }

    @Test
    fun `list filters by method`() {
        record(method = GET)
        record(method = POST)

        assertThat(
            store.list(TransactionFilter(method = "GET")).size,
            equalTo(1)
        )
    }

    @Test
    fun `list filters by status regex`() {
        record(status = OK)
        record(status = NOT_FOUND)

        assertThat(
            store.list(TransactionFilter(status = "4..")).size,
            equalTo(1)
        )
    }

    @Test
    fun `list filters by path substring`() {
        record(uri = "/foo/bar")
        record(uri = "/baz")

        assertThat(
            store.list(TransactionFilter(path = "foo")).size,
            equalTo(1)
        )
    }

    @Test
    fun `list respects limit`() {
        repeat(10) { record() }

        assertThat(
            store.list(limit = 3).size,
            equalTo(3)
        )
    }

    @Test
    fun `list combines filter and limit`() {
        repeat(5) { record(method = GET) }
        repeat(5) { record(method = POST) }

        assertThat(
            store.list(TransactionFilter(method = "GET"), limit = 2).size,
            equalTo(2)
        )
    }
}
