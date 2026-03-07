package org.http4k.wiretap.traffic

import org.http4k.core.HttpTransaction
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.testing.Approver
import org.http4k.wiretap.HttpWiretapFunctionContract
import org.http4k.wiretap.McpWiretapFunctionContract
import org.http4k.wiretap.domain.Direction.Inbound
import org.http4k.wiretap.domain.TransactionStore
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant

class ListTransactionsTest : HttpWiretapFunctionContract, McpWiretapFunctionContract {

    override val toolName = "list_transactions"

    private val store = TransactionStore.InMemory()

    override val function = ListTransactions(store, Clock.systemUTC())

    private fun record() = store.record(
        HttpTransaction(Request(GET, "/test"), Response(OK), Duration.ofMillis(10), start = Instant.EPOCH),
        Inbound
    )

    @Test
    fun `http lists transactions`(approver: Approver) {
        record()

        approver.assertApproved(
            httpClient()(
                Request(POST, "/list")
                    .body("""{"direction":"","host":"","method":"","status":"","path":""}""")
            )
        )
    }

    @Test
    fun `http returns empty for no transactions`(approver: Approver) {
        approver.assertApproved(
            httpClient()(
                Request(POST, "/list")
                    .body("""{"direction":"","host":"","method":"","status":"","path":""}""")
            )
        )
    }

    @Test
    fun `mcp lists transactions`(approver: Approver) {
        record()
        approver.assertToolResponse(emptyMap())
    }

    @Test
    fun `mcp returns empty for no transactions`(approver: Approver) {
        approver.assertToolResponse(emptyMap())
    }
}
