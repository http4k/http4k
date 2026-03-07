package org.http4k.wiretap.traffic

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.HttpTransaction
import org.http4k.core.Method.DELETE
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.testing.Approver
import org.http4k.wiretap.HttpWiretapFunctionContract
import org.http4k.wiretap.McpWiretapFunctionContract
import org.http4k.wiretap.domain.Direction.Inbound
import org.http4k.wiretap.domain.TransactionStore
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant

class ClearTransactionsTest : HttpWiretapFunctionContract, McpWiretapFunctionContract {

    override val toolName = "clear_transactions"

    private val store = TransactionStore.InMemory()

    override val function = ClearTransaction(store)

    private fun record() = store.record(
        HttpTransaction(Request(GET, "/test"), Response(OK), Duration.ofMillis(10), start = Instant.now()),
        Inbound
    )

    @Test
    fun `http clears all transactions`(approver: Approver) {
        record()
        record()

        approver.assertApproved(httpClient()(Request(DELETE, "/")))

        assertThat(store.list().size, equalTo(0))
    }

    @Test
    fun `mcp clears all transactions`(approver: Approver) {
        record()
        record()

        approver.assertToolResponse(emptyMap())

        assertThat(store.list().size, equalTo(0))
    }
}
