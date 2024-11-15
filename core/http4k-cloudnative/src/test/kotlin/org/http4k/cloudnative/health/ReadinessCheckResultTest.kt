package org.http4k.cloudnative.health

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test

class ReadinessCheckResultTest {

    private val success = Completed("name")
    private val failure = Failed("name2", "foobar")

    @Test
    fun `simple result`() {
        assertThat(success.pass, equalTo(true))
        assertThat(failure.pass, equalTo(false))
        assertThat(success, equalTo(Completed("name")))
    }

    @Test
    fun `composite result collects results`() {
        assertThat((success + success).pass, equalTo(true))
        assertThat((failure + success).pass, equalTo(false))
    }
}
