package org.http4k.chaos

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.present
import org.http4k.chaos.ChaosPolicy.Companion.Always
import org.http4k.core.HttpTransaction
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.lens.Header
import org.junit.jupiter.api.Test

class ChaosFiltersTest {
    private val expecteReq = Request(GET, "")

    @Test
    fun `filter with injection policy should apply behaviour and set outgoing header`() {
        val injectedResponse = Always.inject(
                object : ChaosBehaviour {
                    override fun invoke(tx: HttpTransaction) = tx.response.with(Header.Common.CHAOS of "foo")
                }
        )
                .asFilter().then { Response(OK) }(expecteReq)
        assertThat(injectedResponse.header("x-http4k-chaos"), present(equalTo("foo")))
    }
}
