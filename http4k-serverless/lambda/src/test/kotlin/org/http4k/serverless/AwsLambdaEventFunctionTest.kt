package org.http4k.serverless

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.util.proxy
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream

private class MyFunction : AwsLambdaEventFunction(FnLoader {
    FnHandler { _: ScheduledEvent, _: Context ->
        "hello"
    }
})

class AwsLambdaEventFunctionTest {
    @Test
    fun `can implement function and call it`() {
        val input = "{}"
        val output = ByteArrayOutputStream()
        MyFunction().handleRequest(input.byteInputStream(), output, proxy())
        assertThat(output.toString(), equalTo("hello"))
    }
}
