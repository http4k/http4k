package org.http4k.serverless

import com.google.cloud.functions.Context
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.mock4k.mock
import org.http4k.format.Moshi
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicReference

data class MyEvent(val value: String)

class GoogleCloudEventFunctionTest {

    @Test
    fun `can build function and call it`() {
        val captured = AtomicReference<MyEvent>()
        val input = MyEvent("hello")
        val function = object : GoogleCloudEventFunction(FnLoader {
            FnHandler { e: MyEvent, _: Context ->
                captured.set(e)
            }
        }) {}

        function.accept(Moshi.asFormatString(input), mock())
        assertThat(captured.get(), equalTo(input))
    }
}
