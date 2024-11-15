package org.http4k.connect

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.chaos.ChaoticHttpHandler
import org.http4k.chaos.defaultPort
import org.junit.jupiter.api.Test

class FakeSystem1 : ChaoticHttpHandler() {
    override val app = TODO()
}

class FakeSystem2 : ChaoticHttpHandler() {
    override val app = TODO()
}

class DefaultPortFinderTest {
    @Test
    fun `find default port numbers`() {
        assertThat(FakeSystem1::class.defaultPort, equalTo(59155))
        assertThat(FakeSystem2::class.defaultPort, equalTo(25393))
    }
}
