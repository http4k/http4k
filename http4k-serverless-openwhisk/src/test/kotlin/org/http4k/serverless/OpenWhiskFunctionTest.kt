package org.http4k.serverless

import com.google.gson.JsonObject
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.base64Encode
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.format.Gson
import org.junit.jupiter.api.Test

class OpenWhiskFunctionTest {

    @Test
    fun `full request (raw) - calls the handler and returns proper body`() {
        assertExpectedResponseIs(
            FakeOpenWhiskRawRequest(
                "post",
                "/bob",
                "query=qvalue",
                mapOf("header" to "hvalue"),
                "myBody"
            ),
            FakeOpenWhiskResponse(
                200, mapOf(
                    "header" to "hvalue"
                ),
                "/bob?query=qvaluemyBody"
            ),
            { false },
            { false }
        )
    }

    @Test
    fun `full request (with queries at top level) - binary - calls the handler and returns proper body`() {
        assertExpectedResponseIs(
            FakeOpenWhiskRequestWithTopLevelQueries(
                "post",
                "/bob",
                mapOf("header" to "hvalue"),
                "myBody".base64Encode(),
                "qvalue"
            ),
            FakeOpenWhiskResponse(
                200, mapOf(
                    "header" to "hvalue"
                ),
                "L2JvYj9xdWVyeT1xdmFsdWVteUJvZHk="
            ),
            { true },
            { true }
        )
    }

    @Test
    fun `full request (with queries at top level) - non-binary - calls the handler and returns proper body`() {
        assertExpectedResponseIs(
            FakeOpenWhiskRequestWithTopLevelQueries(
                "post",
                "/bob",
                mapOf("header" to "hvalue"),
                "myBody",
                "qvalue"
            ),
            FakeOpenWhiskResponse(
                200, mapOf(
                    "header" to "hvalue"
                ),
                "/bob?query=qvaluemyBody"
            ),
            { false },
            { false }
        )
    }

    @Test
    fun `minimal request - calls the handler and returns proper body`() {
        assertExpectedResponseIs(
            FakeOpenWhiskRequestWithTopLevelQueries("get", null, null, null, null),
            FakeOpenWhiskResponse(200, emptyMap(), "P3F1ZXJ5PQ=="),
            { true },
            { true }
        )
    }

    private fun assertExpectedResponseIs(
        request: Any,
        expected: FakeOpenWhiskResponse,
        isRequestBinary: (Request) -> Boolean,
        isResponseBinary: (Response) -> Boolean
    ) {
        val function = OpenWhiskFunction(object : AppLoader {
            override fun invoke(p1: Map<String, String>) = { req: Request ->
                Response(OK).body(req.uri.toString() + req.bodyString()).headers(req.headers)
            }
        }, isRequestBinary = isRequestBinary, isResponseBinary = isResponseBinary)

        val response = function(Gson.asJsonObject(request) as JsonObject)

        val actual = Gson.asA(response, FakeOpenWhiskResponse::class)

        assertThat(actual.copy(headers = actual.headers.minus("x-http4k-context")), equalTo(expected))
    }
}
