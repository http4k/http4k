package org.http4k.wiretap

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Body
import org.http4k.core.HttpTransaction
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.wiretap.domain.BodyHydration
import org.http4k.wiretap.domain.BodyHydration.All
import org.http4k.wiretap.domain.BodyHydration.None
import org.http4k.wiretap.domain.BodyHydration.RequestOnly
import org.http4k.wiretap.domain.BodyHydration.ResponseOnly
import org.http4k.wiretap.domain.TransactionStore
import org.junit.jupiter.api.Test

class HttpWiretapBodyHydrationTest {

    private val store = TransactionStore.InMemory()

    private fun monitorWith(bodyHydration: BodyHydration): (Request) -> Response {
        val poly = Wiretap.Http(
            transactionStore = store,
            bodyHydration = bodyHydration
        ) { _, _, _ -> { Response(OK).body(Body("response-body".byteInputStream())) } }
        return poly.http!!
    }

    private fun streamRequest() =
        Request(GET, "/").body(Body("request-body".byteInputStream()))

    private fun lastRecordedTransaction(): HttpTransaction =
        store.list().first().transaction

    @Test
    fun `hydrates both request and response bodies when All`() {
        val handler = monitorWith(All)
        handler(streamRequest())

        val tx = lastRecordedTransaction()
        assertThat(tx.request.bodyString(), equalTo("request-body"))
        assertThat(tx.response.bodyString(), equalTo("response-body"))
    }

    @Test
    fun `hydrates only request body when RequestOnly`() {
        val handler = monitorWith(RequestOnly)
        handler(streamRequest())

        val tx = lastRecordedTransaction()
        assertThat(tx.request.bodyString(), equalTo("request-body"))
    }

    @Test
    fun `hydrates only response body when ResponseOnly`() {
        val handler = monitorWith(ResponseOnly)
        handler(streamRequest())

        val tx = lastRecordedTransaction()
        assertThat(tx.response.bodyString(), equalTo("response-body"))
    }

    @Test
    fun `does not hydrate bodies when None`() {
        val handler = monitorWith(None)
        handler(streamRequest())

        assertThat(store.list().size, equalTo(1))
    }
}
