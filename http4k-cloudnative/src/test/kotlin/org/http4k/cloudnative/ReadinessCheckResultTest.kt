package org.http4k.cloudnative

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test

class ReadinessCheckResultTest {

    private val success = ReadinessCheckResult(true)
    private val failure = ReadinessCheckResult(false)

    @Test
    fun `simple result`() {
        assertThat(success.pass, equalTo(true))
        assertThat(failure.pass, equalTo(false))
        assertThat(success, equalTo(ReadinessCheckResult(true)))
    }

    @Test
    fun `composite result collects results`() {
        assertThat((ReadinessCheckResult() + success + success).pass, equalTo(true))
        assertThat((ReadinessCheckResult() + failure + success).pass, equalTo(false))
    }
}