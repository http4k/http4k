package org.http4k.chaos

import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import org.http4k.chaos.ChaosStage.Companion.Wait
import org.http4k.core.HttpTransaction
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.I_M_A_TEAPOT
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.junit.jupiter.api.Test

class ChaosStageTest {
    private val response = Response(OK).body("body")

    @Test
    fun `Wait does not match the response`() {
        val app = Wait.asFilter().then { response }
        app(Request(GET, "")) shouldMatch equalTo(response)
    }

    @Test
    fun `until stops when the trigger is hit`() {
        val app = object : ChaosStage {
            override fun invoke(tx: HttpTransaction) = Response(NOT_FOUND)
        }.until { it.request.method == POST }
                .asFilter().then { response }

        app(Request(GET, "")) shouldMatch equalTo(Response(NOT_FOUND))
        app(Request(POST, "")) shouldMatch equalTo(response)
        app(Request(GET, "")) shouldMatch equalTo(response)
    }

    @Test
    fun `then moves onto the next stage`() {
        val app = object : ChaosStage {
            override fun invoke(tx: HttpTransaction) = Response(I_M_A_TEAPOT)
        }
                .until { it.request.method == POST }
                .then(object : ChaosStage {
                    override fun invoke(tx: HttpTransaction) = Response(NOT_FOUND)
                })
                .asFilter().then { response }

        app(Request(GET, "")) shouldMatch equalTo(Response(I_M_A_TEAPOT))
        app(Request(POST, "")) shouldMatch equalTo(Response(NOT_FOUND))
        app(Request(GET, "")) shouldMatch equalTo(Response(NOT_FOUND))
    }
}
