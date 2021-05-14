package org.http4k.testing

import com.amazonaws.services.lambda.runtime.events.ScheduledEvent
import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.client.JavaHttpClient
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasStatus
import org.http4k.routing.bind
import org.http4k.routing.functions
import org.http4k.server.SunHttp
import org.http4k.server.asServer
import org.http4k.serverless.FnHandler
import org.http4k.serverless.InvocationFnLoader
import org.http4k.serverless.FnLoader
import org.junit.jupiter.api.Test

class FakeAwsLambdaTest {

    @Test
    fun `can launch function with FakeLambda and call it as if directly in lambda`() {
        val functions = functions(
            "aFunction" bind FnLoader {
                FnHandler { _: ScheduledEvent, _ ->
                    "aFunction"
                }
            },
            "anApp" bind InvocationFnLoader {
                Response(OK).body(it.bodyString() + it.bodyString())
            }
        )

        val http = JavaHttpClient()

        FakeAwsLambda(functions).asServer(SunHttp(0)).start().use { it ->
            assertThat(
                http(
                    Request(POST, "http://localhost:${it.port()}/2015-03-31/functions/aFunction/invocations")
                        .body("{}")
                ),
                hasStatus(OK).and(hasBody("aFunction"))
            )

            assertThat(
                http(
                    Request(POST, "http://localhost:${it.port()}/2015-03-31/functions/anApp/invocations")
                        .body("hello")
                ),
                hasStatus(OK).and(hasBody("hellohello"))
            )
        }
    }
}
