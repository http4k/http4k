package org.http4k.serverless

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch

class AwsLambdaRuntimeTest {

    @Test
    fun `enters the loop correctly`() {
        val latch = CountDownLatch(2)

        val lambdaRuntimeApi = FakeLambdaRuntimeApi().apply {
            events += ScheduledEvent().apply {
                account = "helloworld1"
            }
            events += ScheduledEvent().apply {
                account = "helloworld2"
            }
        }
        FnLoader {
            FnHandler { e: ScheduledEvent, _: Context ->
                latch.countDown()
                e.account
            }
        }.asServer(
            AwsLambdaRuntime(
                mapOf("AWS_LAMBDA_RUNTIME_API" to "localhost"),
                lambdaRuntimeApi
            )
        ).start().use {
            latch.await()
        }

        assertThat(lambdaRuntimeApi.responses.first(), equalTo("helloworld1"))
    }
}
