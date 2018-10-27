package org.http4k.k8s

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.SERVICE_UNAVAILABLE
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasStatus
import org.junit.jupiter.api.Test

class HealthTest {
    private val health = Health()

    @Test
    fun liveness() {
        assertThat(health(Request(GET, "/liveness")), hasStatus(OK))
    }

    @Test
    fun readiness() {
        assertThat(health(Request(GET, "/readiness")), hasStatus(OK).and(hasBody("success=true")))
    }

    @Test
    fun `readiness with extra checks`() {
        assertThat(Health(checks = listOf(check(true, "first"), check(false, "second")))(Request(GET, "/readiness")),
            hasStatus(SERVICE_UNAVAILABLE).and(hasBody("success=false\nfirst=true\nsecond=false")))
    }

    private fun check(result: Boolean, name: String): ReadinessCheck = { ReadinessCheckResult(result, name) }
}