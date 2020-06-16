package org.http4k.serverless

import com.google.gson.JsonObject
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.format.Gson
import org.http4k.serverless.openwhisk.OpenWhiskFunction
import org.junit.jupiter.api.Test

class OpenWhiskFunctionTest {

    @Test
    fun `calls the handler and returns proper body`() {
        val app = { req: Request ->
            Response(OK).body(
                req.uri.toString() + req.bodyString()
            ).headers(req.headers)
        }
        val request = FakeOpenWhiskRequest("GET", "/bob",
            mapOf("query" to "qvalue"),
            mapOf("header" to "hvalue"),
            "bob"
        )

        val function = OpenWhiskFunction(object : AppLoader {
            override fun invoke(p1: Map<String, String>) = app
        })

        val response = function.main(Gson.asJsonObject(request) as JsonObject)

        val actual = Gson.asA(response, FakeOpenWhiskResponse::class)
        assertThat(actual,
            equalTo(FakeOpenWhiskResponse(200, mapOf(
                "x-http4k-context" to actual.headers["x-http4k-context"]!!,
                "header" to "hvalue"),
                "/bob?query=qvalue")))
    }
}
