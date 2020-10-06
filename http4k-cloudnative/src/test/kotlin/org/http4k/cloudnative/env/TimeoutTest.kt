package org.http4k.cloudnative.env

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.throws
import org.junit.jupiter.api.Test
import java.time.Duration

class TimeoutTest {
    @Test
    fun `timeout value`() {
        assertThat({ Timeout(Duration.ofMillis(-1)) }, throws<IllegalArgumentException>())
    }
}
