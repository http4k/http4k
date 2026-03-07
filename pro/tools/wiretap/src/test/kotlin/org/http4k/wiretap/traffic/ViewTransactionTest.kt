package org.http4k.wiretap.traffic

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.HttpTransaction
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.testing.Approver
import org.http4k.wiretap.HttpWiretapFunctionContract
import org.http4k.wiretap.McpWiretapFunctionContract
import org.http4k.wiretap.domain.Direction.Inbound
import org.http4k.wiretap.domain.TransactionStore
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant

class ViewTransactionTest : HttpWiretapFunctionContract, McpWiretapFunctionContract {

    override val toolName = "get_transaction"

    private val store = TransactionStore.InMemory()

    override val function = ViewTransaction(store)

    private fun record() = store.record(
        HttpTransaction(Request(GET, "/test"), Response(OK), Duration.ofMillis(10), start = Instant.EPOCH),
        Inbound
    )

    @Test
    fun `http returns transaction detail`(approver: Approver) {
        val tx = record()
        approver.assertApproved(httpClient()(Request(GET, "/${tx.id}")))
    }

    @Test
    fun `http returns 404 for unknown id`() {
        val response = httpClient()(Request(GET, "/999"))
        assertThat(response.status, equalTo(NOT_FOUND))
    }

    @Test
    fun `mcp returns transaction detail`(approver: Approver) {
        val tx = record()
        approver.assertToolResponse(mapOf("id" to tx.id))
    }

    @Test
    fun `mcp returns error for unknown id`(approver: Approver) {
        approver.assertToolResponse(mapOf("id" to 999L))
    }
}
