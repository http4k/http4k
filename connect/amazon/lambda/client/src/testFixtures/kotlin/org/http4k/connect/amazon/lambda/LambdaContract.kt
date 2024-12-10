package org.http4k.connect.amazon.lambda

import com.amazonaws.services.lambda.runtime.events.ScheduledEvent
import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.Success
import dev.forkhandles.result4k.map
import org.http4k.client.JavaHttpClient
import org.http4k.connect.amazon.AwsContract
import org.http4k.connect.amazon.lambda.action.invokeFunction
import org.http4k.connect.amazon.lambda.action.invokeStreamFunction
import org.http4k.connect.amazon.lambda.model.FunctionName
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.OK
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasStatus
import org.http4k.server.SunHttp
import org.http4k.server.asServer
import org.http4k.util.PortBasedTest
import org.junit.jupiter.api.Test

interface LambdaContract : AwsContract, PortBasedTest {
    private val lambda
        get() =
        Lambda.Http(aws.region, { aws.credentials }, http)

    @Test
    fun `can use invokeFunction with an automarshalled event`() {
        val input = ScheduledEvent().apply {
            account = "hello world"
        }
        assertThat(lambda.invokeFunction(FunctionName.of("event"), input), equalTo(Success(input)))
    }

    @Test
    fun `can use invokeStreamFunction`() {
        assertThat(
            lambda.invokeStreamFunction(FunctionName.of("stream"), "hello".byteInputStream())
                .map { it.reader().readText() },
            equalTo(Success("hello"))
        )
    }

    @Test
    fun `can invoke function over http`() {
        http.asServer(SunHttp(0)).start().use { it ->
            assertThat(
                JavaHttpClient()(
                    Request(POST, "http://localhost:${it.port()}/2015-03-31/functions/http/invocations")
                        .body("hello")
                ),
                hasStatus(OK).and(hasBody("hellohello"))
            )
        }
    }
}
