package org.http4k.k8s

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.junit.jupiter.api.Test

class DefaultReadinessCheckResultRendererTest{

    @Test
    fun `calls toString() on result`() {
        assertThat(DefaultReadinessCheckResultRenderer(ReadinessCheckResult(true)), equalTo(Response(OK).body("success=true")))
    }
}