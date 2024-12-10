package org.http4k.k8s.health

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.cloudnative.health.Completed
import org.http4k.cloudnative.health.DefaultReadinessCheckResultRenderer
import org.http4k.cloudnative.health.Failed
import org.http4k.cloudnative.health.plus
import org.junit.jupiter.api.Test

class DefaultReadinessCheckResultRendererTest {

    @Test
    fun `calls toString() on result`() {
        assertThat(
            DefaultReadinessCheckResultRenderer(
            Failed("first", "failed")
                + Completed("second")
        ), equalTo("overall=false\nfirst=false [failed]\nsecond=true"))
    }
}
