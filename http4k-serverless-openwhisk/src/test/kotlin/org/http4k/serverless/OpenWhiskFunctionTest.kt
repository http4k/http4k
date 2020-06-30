package org.http4k.serverless

import com.google.gson.JsonObject
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.format.Gson
import org.junit.jupiter.api.Test

class OpenWhiskFunctionTest {

    @Test
    fun `full request - calls the handler and returns proper body`() {
        assertExpectedResponseIs(
            FakeOpenWhiskRequest("post", "/bob", mapOf("query" to "qvalue"), mapOf("header" to "hvalue"), "bob"),
            FakeOpenWhiskResponse(200, mapOf(
                "header" to "hvalue"),
                "/bob?query=qvaluebob")
        )
    }

    @Test
    // see https://github.com/apache/openwhisk-runtime-java/issues/111
    fun `full request (with queries bug) - calls the handler and returns proper body`() {
        assertExpectedResponseIs(
            FakeOpenWhiskRequestWithIncorrectQueries("post", "/bob", mapOf("header" to "hvalue"), "bob", "qvalue"),
            FakeOpenWhiskResponse(200, mapOf(
                "header" to "hvalue"),
                "/bob?query=qvaluebob")
        )
    }

    @Test
    fun `minimal request - calls the handler and returns proper body`() {
        assertExpectedResponseIs(
            FakeOpenWhiskRequest("get", null, null, null, null),
            FakeOpenWhiskResponse(200, emptyMap(), "")
        )
    }

    private fun assertExpectedResponseIs(request: Any, expected: FakeOpenWhiskResponse) {
        val app = { req: Request ->
            Response(OK).body(
                req.uri.toString() + req.bodyString()
            ).headers(req.headers)
        }

        val function = OpenWhiskFunction(object : AppLoader {
            override fun invoke(p1: Map<String, String>) = app
        })

        val response = function(Gson.asJsonObject(request) as JsonObject)

        val actual = Gson.asA(response, FakeOpenWhiskResponse::class)

        assertThat(actual.copy(headers = actual.headers.minus("x-http4k-context")), equalTo(expected))
    }
}
