package org.http4k.wiretap.traffic

import org.http4k.core.HttpTransaction
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.testing.Approver
import org.http4k.wiretap.HttpWiretapFunctionContract
import org.http4k.wiretap.domain.Direction.Inbound
import org.http4k.wiretap.domain.TransactionFilter
import org.http4k.wiretap.domain.TransactionStore
import org.http4k.wiretap.domain.ViewStore
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant

class ActivateViewTest : HttpWiretapFunctionContract {

    private val views = ViewStore.InMemory()
    private val store = TransactionStore.InMemory()

    override val function = ActivateView(views, store)

    private fun record(uri: String = "/test") = store.record(
        HttpTransaction(Request(GET, uri), Response(OK), Duration.ofMillis(10), start = Instant.EPOCH),
        Inbound
    )

    @Test
    fun `http activates filtered view`(approver: Approver) {
        val view = views.add("API", TransactionFilter(path = "/api"))

        record(uri = "/api/users")
        record(uri = "/other")

        approver.assertApproved(
            httpClient()(Request(POST, "/views/${view.id}/activate"))
        )
    }
}
