package org.http4k.cloudnative

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test

class DefaultReadinessCheckResultRendererTest {

    @Test
    fun `calls toString() on result`() {
        assertThat(DefaultReadinessCheckResultRenderer(
                Failed("first", "failed")
                + Completed("second")
        ), equalTo("success=false\nfirst=false [failed]\nsecond=true"))
    }
}