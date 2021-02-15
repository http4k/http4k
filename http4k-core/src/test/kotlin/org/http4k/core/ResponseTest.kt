package org.http4k.core

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Status.Companion.OK
import org.junit.jupiter.api.Test

class ResponseTest {
    @Test
    fun `differences in header order do not invalidate equality`() {
        val responseOne = Response(OK).header("foo", "bar").header("fizz", "buzz")
        val responseTwo = Response(OK).header("fizz", "buzz").header("foo", "bar")
        assertThat(responseOne, equalTo(responseTwo))
    }

    @Test
    fun `if multiple headers with the same key exist in two headers, they must be in the same order for the headers to be equal`() {
        val responseOne = Response(OK)
            .header("foo", "bar")
            .header("foo", "buzz")
            .header("fizz", "bar")
            .header("fizz", "buzz")
            .header("Content-Type", "application/json")
        val responseTwo = Response(OK)
            .header("foo", "buzz")
            .header("foo", "bar")
            .header("fizz", "bar")
            .header("fizz", "buzz")
            .header("Content-Type", "application/json")
        val responseThree = Response(OK)
            .header("Content-Type", "application/json")
            .header("fizz", "bar")
            .header("fizz", "buzz")
            .header("foo", "buzz")
            .header("foo", "bar")

        assertThat(responseOne, !equalTo(responseTwo))
        assertThat(responseTwo, equalTo(responseThree))
    }
}
