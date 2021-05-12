package org.http4k.serverless

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test

class FnFilterTest {

    private val messages = mutableListOf<String>()

    private val inner = FnHandler { msg: String, ctx: String ->
        messages += "first filter in"
        msg + msg
    }

    private val first = FnFilter { next: FnHandler<String, String, String> ->
        FnHandler { msg, ctx ->
            messages += "first filter in"
            next(msg + msg.reversed(), ctx).also { messages += "first filter out" }
        }
    }

    private val second = FnFilter { next: FnHandler<String, String, String> ->
        FnHandler { msg, ctx ->
            messages += "second filter in"
            next(msg, ctx).let {
                messages += "second filter out"
                "$it!$it"
            }
        }
    }

    @Test
    fun `can manipulate value on way in and out of function`() {
        val svc = first.then(second).then(inner)
        assertThat(svc("hello", "ctx"), equalTo("helloollehhelloolleh!helloollehhelloolleh"))
        assertThat(
            messages, equalTo(
                listOf(
                    "first filter in", "second filter in", "first filter in", "second filter out", "first filter out"
                )
            )
        )
    }
}
