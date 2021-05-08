package org.http4k.testing

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent
import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.client.JavaHttpClient
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.OK
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasStatus
import org.http4k.routing.bind
import org.http4k.routing.functions
import org.http4k.server.SunHttp
import org.http4k.server.asServer
import org.http4k.serverless.FunctionHandler
import org.http4k.serverless.FunctionLoader
import org.http4k.serverless.lambda.invoke
import org.junit.jupiter.api.Test

class FakeAwsLambdaTest {
    @Test
    fun `can launch function with FakeLambda and call it as if directly in lambda`() {
        val functions = functions(
            "function1" bind FunctionLoader {
                FunctionHandler { e: ScheduledEvent, _: Context ->
                    "function1"
                }
            },
            "function2" bind FunctionLoader {
                FunctionHandler { e: ScheduledEvent, _: Context ->
                    "function2"
                }
            }
        )

        val http = JavaHttpClient()

        FakeAwsLambda(functions).asServer(SunHttp(0)).start().use { it ->
            assertThat(
                http(
                    Request(POST, "http://localhost:${it.port()}/2015-03-31/functions/function1/invocations")
                        .body("{}")
                ),
                hasStatus(OK).and(hasBody("function1"))
            )
        }
    }
}
