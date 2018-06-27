package org.http4k.chaos

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.present
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.lens.Header
import org.junit.Test

class ChaosFiltersTest {
    private val expecteReq = Request(GET, "")

    @Test
    fun `filter with request injection policy should apply behaviour on request`() {
        val injectedResponse = ChaosFilters(
                ChaosPolicy.Always().inject(
                        object : ChaosBehaviour {
                            override fun invoke(request: Request) = request.also { assertThat(it, equalTo(expecteReq)) }
                            override fun invoke(response: Response) = response.with(Header.Common.CHAOS of "foo")
                        }
                )
        ).then { Response(OK) }(expecteReq)
        assertThat(injectedResponse.header("x-http4k-chaos"), present(equalTo("foo")))
    }

    @Test
    fun `filter with response injection policy should apply behaviour on response`() {
        val injectedResponse = ChaosFilters(
                ChaosPolicy.Always(injectRequest = false).inject(
                        object : ChaosBehaviour {
                            override fun invoke(response: Response) = response
                                    .also { assertThat(it, equalTo(Response(OK))) }.with(Header.Common.CHAOS of "foo")
                        })
        ).then { Response(OK) }(expecteReq)
        assertThat(injectedResponse.header("x-http4k-chaos"), present(equalTo("foo")))

    }
}
