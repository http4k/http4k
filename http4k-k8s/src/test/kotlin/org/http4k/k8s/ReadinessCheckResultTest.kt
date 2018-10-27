package org.http4k.k8s

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.k8s.ReadinessCheckResult.Companion.Composite
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
        assertThat((Composite() + success + success).pass, equalTo(true))
        assertThat((Composite() + failure + success).pass, equalTo(false))
    }
}