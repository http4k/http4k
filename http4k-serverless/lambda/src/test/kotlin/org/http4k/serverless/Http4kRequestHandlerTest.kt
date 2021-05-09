package org.http4k.serverless

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream

private class MyFunction : Http4kRequestHandler(FunctionLoader {
    FunctionHandler { e: ScheduledEvent, _: Context ->
        "hello"
    }
})

class Http4kRequestHandlerTest {
    @Test
    fun `can implement function and call it`() {
        val input = "{}"
        val output = ByteArrayOutputStream()
        MyFunction().handleRequest(input.byteInputStream(), output, proxy())
        assertThat(output.toString(), equalTo("hello"))
    }
}
