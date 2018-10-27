package org.http4k.k8s

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test

class DefaultReadinessCheckResultRendererTest {

    @Test
    fun `calls toString() on result`() {
        assertThat(DefaultReadinessCheckResultRenderer(
            ReadinessCheckResult(false, "overall")
                + ReadinessCheckResult(false, "first")
                + ReadinessCheckResult(true, "second")
        ), equalTo("success=false\nsuccess=false\nsecond=true"))
    }
}