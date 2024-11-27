package org.http4k.serverless

import com.amazonaws.services.lambda.runtime.events.ScheduledEvent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant.EPOCH
import java.time.ZoneId
import java.util.UUID

class LambdaRuntimeAPITest {

    private val fake = FakeLambdaRuntimeApi(Clock.fixed(EPOCH, ZoneId.of("UTC"))) { UUID(0, 0) }.apply {
        events.add(ScheduledEvent().apply {
            account = "helloworld"
        })
    }

    private val lambdaRuntimeAPI = LambdaRuntimeAPI(fake)

    @Test
    fun `get next invocation`() {
        assertThat(
            lambdaRuntimeAPI.nextInvocation(), equalTo(
                Request(POST, "")
                    .header("content-type", "application/json; charset=utf-8")
                    .header("Lambda-Runtime-Aws-Request-Id", "00000000-0000-0000-0000-000000000000")
                    .header("Lambda-Runtime-Trace-Id", "00000000-0000-0000-0000-000000000000")
                    .header(
                        "Lambda-Runtime-Invoked-Function-Arn",
                        "arn:aws:lambda:eu-west-2:1234567890:function:function"
                    )
                    .header("Lambda-Runtime-Deadline-Ms", "900000")
                    .body("""{"account":"helloworld"}""")
            )
        )
    }

    @Test
    fun `post init error`() {
        val error = Exception()
        LambdaRuntimeAPI(fake).initError(error)
        assertThat(fake.errors[0], equalTo(error.toBody()))
    }

    @Test
    fun `post request error`() {
        val error = Exception()
        LambdaRuntimeAPI(fake).error(
            Request(POST, "")
                .header("Lambda-Runtime-Aws-Request-Id", "00000000-0000-0000-0000-000000000000"), error
        )
        assertThat(fake.errors[0], equalTo(error.toBody()))
    }

    @Test
    fun `post request response`() {
        LambdaRuntimeAPI(fake).success(
            Request(POST, "")
                .header("Lambda-Runtime-Aws-Request-Id", "00000000-0000-0000-0000-000000000000"),
            "hello".byteInputStream()
        )
        assertThat(fake.responses[0], equalTo("hello"))
    }
}
