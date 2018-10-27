package org.http4k.k8s

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.junit.jupiter.api.Test

class HealthTest {
    @Test
    fun liveness() {
        assertThat(Health()(Request(GET, "/liveness")), equalTo(Response(OK)))
    }

    @Test
    fun readiness() {
        assertThat(Health()(Request(GET, "/readiness")), equalTo(Response(OK)))
    }

}