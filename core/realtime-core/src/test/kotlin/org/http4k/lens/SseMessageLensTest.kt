package org.http4k.lens

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.with
import org.http4k.sse.SseMessage.Data
import org.junit.jupiter.api.Test

class SseMessageLensTest {

    @Test
    fun `extract and inject data`() {
        Request(Method.GET, "").with()
        val lens = SseMessage.csv()
            .map({ it.map { it.split(":").let { it[0] to it[1] } } }, { it.map { (k, v) -> "$k:$v" } })
            .required("foo")

        val input = listOf("1" to "b")
        val injected = Data().with(lens of input)
        assertThat(injected.data, equalTo("1:b"))
        assertThat(lens(injected), equalTo(input))
    }
}
