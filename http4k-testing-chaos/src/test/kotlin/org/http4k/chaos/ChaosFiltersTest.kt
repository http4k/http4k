package org.http4k.chaos

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.present
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.junit.Test

class ChaosFiltersTest {
    private val expecteReq = Request(GET, "")
    private val expectedResponse = Response(OK)

    @Test
    fun `filter with request injection policy should apply behaviour on request`() {
        val injectedResponse = ChaosFilters(
                ChaosPolicy.Always(),
                object : ChaosBehaviour {
                    override val description = "foo"
                    override fun inject(request: Request): Request = request.also { assertThat(it, equalTo(expecteReq)) }
                }
        ).then { expectedResponse }(expecteReq)
        assertThat(injectedResponse.header("x-http4k-chaos"), present(equalTo("foo")))
    }

    @Test
    fun `filter with response injection policy should apply behaviour on response`() {
        val injectedResponse = ChaosFilters(
                ChaosPolicy.Always(injectRequest = false),
                object : ChaosBehaviour {
                    override val description = "foo"
                    override fun inject(response: Response): Response = response.also { assertThat(it, equalTo(expectedResponse)) }
                }
        ).then { expectedResponse }(expecteReq)
        assertThat(injectedResponse.header("x-http4k-chaos"), present(equalTo("foo")))
    }
}
